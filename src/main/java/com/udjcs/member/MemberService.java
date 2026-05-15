package com.udjcs.member;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;

@Service
@Transactional
public class MemberService {

    private static final Logger log = LoggerFactory.getLogger(MemberService.class);

    private final MemberRepository repository;
    private final QrCodeService qrCodeService;
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public MemberService(MemberRepository repository,
                         QrCodeService qrCodeService,
                         JavaMailSender mailSender) {
        this.repository = repository;
        this.qrCodeService = qrCodeService;
        this.mailSender = mailSender;
    }

    public List<Member> findAll() {
        return repository.findAll(Sort.by(Sort.Order.asc("lastName"), Sort.Order.asc("firstName")));
    }

    public Member findById(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Member not found: " + id));
    }

    public void save(Member member) {
        repository.save(member);
    }

    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    public List<Member> findPending() {
        return repository.findByApprovalStatusOrderByCreatedAtAsc("Pending");
    }

    public long countPending() {
        return repository.countByApprovalStatus("Pending");
    }

    public void approve(Long id) {
        Member member = findById(id);
        member.setApprovalStatus("Approved");
        member.setMembershipType("General");

        byte[] qr = qrCodeService.generate(buildQrContent(member));
        member.setQrCode(qr);
        repository.save(member);

        if (member.getEmail() != null && !member.getEmail().isBlank()) {
            try {
                sendApprovalEmail(member, qr);
            } catch (Exception e) {
                log.error("Failed to send approval email to {}: {}", member.getEmail(), e.getMessage());
            }
        }
    }

    public Member updateProfile(Long id, String address, String phone, String email) {
        Member member = findById(id);
        member.setAddress(address);
        member.setPhone(phone);
        member.setEmail(email != null && !email.isBlank() ? email : null);
        return repository.save(member);
    }

    public void savePhoto(Long id, MultipartFile file) throws IOException {
        Member member = findById(id);
        member.setProfilePicture(file.getBytes());
        member.setPhotoMimeType(file.getContentType());
        repository.save(member);
    }

    public void deletePhoto(Long id) {
        Member member = findById(id);
        member.setProfilePicture(null);
        member.setPhotoMimeType(null);
        repository.save(member);
    }

    private String buildQrContent(Member member) {
        return "UDJCS MEMBER CARD\n" +
               "ID: " + member.getId() + "\n" +
               "Name: " + member.getFirstName() + " " + member.getLastName() + "\n" +
               "Membership: " + member.getMembershipType() + "\n" +
               "Phone: " + member.getPhone() + "\n" +
               "Since: " + member.getMembershipDate();
    }

    private void sendApprovalEmail(Member member, byte[] qrBytes) throws MessagingException {
        MimeMessage msg = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");

        helper.setFrom(fromEmail);
        helper.setTo(member.getEmail());
        helper.setSubject("UDJCS Membership Approved – Welcome, " + member.getFirstName() + "!");

        String fullName = member.getFirstName() + " " + member.getLastName();
        String body = "<html><body style=\"font-family:Segoe UI,Arial,sans-serif;color:#1a1a2e;margin:0;padding:0;\">" +
            "<div style=\"max-width:540px;margin:32px auto;border-radius:12px;overflow:hidden;box-shadow:0 4px 24px rgba(0,0,0,0.10);\">" +
            "<div style=\"background:linear-gradient(135deg,#1a1a2e 0%,#0f3460 100%);padding:28px 32px;\">" +
            "<h2 style=\"color:#fff;margin:0;font-size:1.3rem;\">United Digambar Jain Community</h2>" +
            "<p style=\"color:rgba(255,255,255,0.65);margin:4px 0 0;font-size:0.85rem;\">Trust Management System</p>" +
            "</div>" +
            "<div style=\"padding:28px 32px;background:#fff;\">" +
            "<h3 style=\"color:#e94560;margin-top:0;\">Membership Approved!</h3>" +
            "<p>Dear <strong>" + fullName + "</strong>,</p>" +
            "<p>Your membership application has been reviewed and <strong>approved</strong>. Welcome to the United Digambar Jain Community!</p>" +
            "<table style=\"width:100%;border-collapse:collapse;margin:20px 0;font-size:0.9rem;\">" +
            "<tr style=\"background:#f8f9fa;\"><td style=\"padding:8px 12px;font-weight:600;width:40%;\">Member ID</td><td style=\"padding:8px 12px;\">" + member.getId() + "</td></tr>" +
            "<tr><td style=\"padding:8px 12px;font-weight:600;\">Full Name</td><td style=\"padding:8px 12px;\">" + fullName + "</td></tr>" +
            "<tr style=\"background:#f8f9fa;\"><td style=\"padding:8px 12px;font-weight:600;\">Membership Type</td><td style=\"padding:8px 12px;\">" + member.getMembershipType() + "</td></tr>" +
            "<tr><td style=\"padding:8px 12px;font-weight:600;\">Member Since</td><td style=\"padding:8px 12px;\">" + member.getMembershipDate() + "</td></tr>" +
            "<tr style=\"background:#f8f9fa;\"><td style=\"padding:8px 12px;font-weight:600;\">Phone</td><td style=\"padding:8px 12px;\">" + member.getPhone() + "</td></tr>" +
            "</table>" +
            "<p style=\"margin-top:20px;\">Your membership QR code is attached below. Please save it — it can be used for identity verification at community events.</p>" +
            "<div style=\"text-align:center;margin:24px 0;\">" +
            "<img src=\"cid:memberQr\" alt=\"Member QR Code\" style=\"width:200px;height:200px;border:1px solid #dee2e6;border-radius:8px;padding:8px;\">" +
            "</div>" +
            "<p style=\"color:#64748b;font-size:0.82rem;margin-top:24px;\">If you have any questions, please contact the trust administration.</p>" +
            "</div>" +
            "<div style=\"background:#f0f2f5;padding:16px 32px;text-align:center;\">" +
            "<p style=\"color:#94a3b8;font-size:0.78rem;margin:0;\">© United Digambar Jain Community System · Trust Management</p>" +
            "</div>" +
            "</div></body></html>";

        helper.setText(body, true);
        helper.addInline("memberQr", new org.springframework.core.io.ByteArrayResource(qrBytes), "image/png");

        mailSender.send(msg);
    }
}
