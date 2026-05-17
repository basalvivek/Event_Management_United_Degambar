package com.udjcs.invitation;

import com.udjcs.member.QrCodeService;
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

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@Transactional
public class InvitationRegistrationService {

    private static final Logger log = LoggerFactory.getLogger(InvitationRegistrationService.class);

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.base.url:http://localhost:8082}")
    private String baseUrl;

    private final InvitationRegistrationRepository repository;
    private final InvitationRepository invitationRepository;
    private final QrCodeService qrCodeService;
    private final JavaMailSender mailSender;

    public InvitationRegistrationService(InvitationRegistrationRepository repository,
                                          InvitationRepository invitationRepository,
                                          QrCodeService qrCodeService,
                                          JavaMailSender mailSender) {
        this.repository = repository;
        this.invitationRepository = invitationRepository;
        this.qrCodeService = qrCodeService;
        this.mailSender = mailSender;
    }

    public List<InvitationRegistration> findByInvitationId(Long invitationId) {
        return repository.findByInvitationId(invitationId, Sort.by(Sort.Direction.DESC, "id"));
    }

    public long countByInvitationId(Long invitationId) {
        return repository.countByInvitationId(invitationId);
    }

    public void register(String slug, InvitationRegistration reg) {
        Invitation inv = invitationRepository.findBySlugWithDetails(slug)
                .orElseThrow(() -> new IllegalArgumentException("Invitation not found."));
        reg.setInvitation(inv);

        BigDecimal total = BigDecimal.ZERO;
        int adult  = reg.getAttendeesAdult()  != null ? reg.getAttendeesAdult()  : 0;
        int youth  = reg.getAttendeesYouth()  != null ? reg.getAttendeesYouth()  : 0;
        int under8 = reg.getAttendeesUnder8() != null ? reg.getAttendeesUnder8() : 0;

        if (inv.getTicketPriceAdult()  != null) total = total.add(inv.getTicketPriceAdult().multiply(BigDecimal.valueOf(adult)));
        if (inv.getTicketPriceYouth()  != null) total = total.add(inv.getTicketPriceYouth().multiply(BigDecimal.valueOf(youth)));
        if (inv.getTicketPriceUnder8() != null) total = total.add(inv.getTicketPriceUnder8().multiply(BigDecimal.valueOf(under8)));
        if (reg.getDonationAmount()    != null) total = total.add(reg.getDonationAmount());

        reg.setTotalAmount(total);
        repository.save(reg);

        if (reg.getEmail() != null && !reg.getEmail().isBlank()) {
            try {
                sendConfirmationEmail(reg, inv);
            } catch (Exception e) {
                log.error("Failed to send registration email to {}: {}", reg.getEmail(), e.getMessage());
            }
        }
    }

    private void sendConfirmationEmail(InvitationRegistration reg, Invitation inv) throws MessagingException {
        String fullName  = reg.getFirstName() + " " + reg.getLastName();
        String eventDate = inv.getEventDate() != null
                ? inv.getEventDate().format(DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy")) : "";
        String venue     = (inv.getAddress() != null ? inv.getAddress() : "") +
                           (inv.getPostcode() != null && !inv.getPostcode().isBlank() ? ", " + inv.getPostcode() : "");
        String publicUrl = baseUrl + "/e/" + inv.getSlug();

        String qrContent = "UDJCS EVENT REGISTRATION\n" +
                "Event: " + inv.getEventTitle() + "\n" +
                "Name: " + fullName + "\n" +
                "Date: " + eventDate + "\n" +
                "Adults: " + safeInt(reg.getAttendeesAdult()) +
                " | Youth: " + safeInt(reg.getAttendeesYouth()) +
                " | Under 8: " + safeInt(reg.getAttendeesUnder8()) + "\n" +
                "Total: £" + (reg.getTotalAmount() != null ? reg.getTotalAmount().toPlainString() : "0.00") + "\n" +
                "Ref: REG-" + reg.getId();

        byte[] qrBytes = qrCodeService.generate(qrContent);

        MimeMessage msg = mailSender.createMimeMessage();
        MimeMessageHelper h = new MimeMessageHelper(msg, true, "UTF-8");
        h.setFrom(fromEmail);
        h.setTo(reg.getEmail());
        h.setSubject("Registration Confirmed – " + inv.getEventTitle());

        String orgName = inv.getOrganization() != null ? inv.getOrganization().getName() : "United Digambar Jain Community";
        String totalStr = "£" + (reg.getTotalAmount() != null ? reg.getTotalAmount().toPlainString() : "0.00");

        String body =
            "<html><body style='font-family:Segoe UI,Arial,sans-serif;color:#1e1b4b;margin:0;padding:0;'>" +
            "<div style='max-width:560px;margin:28px auto;border-radius:14px;overflow:hidden;box-shadow:0 4px 24px rgba(0,0,0,0.10);'>" +

            "<div style='background:linear-gradient(135deg,#4f46e5 0%,#7c3aed 100%);padding:28px 32px;'>" +
            "<h2 style='color:#fff;margin:0;font-size:1.2rem;'>" + orgName + "</h2>" +
            "<p style='color:rgba(255,255,255,0.65);margin:4px 0 0;font-size:0.82rem;'>Event Registration Confirmation</p>" +
            "</div>" +

            "<div style='padding:28px 32px;background:#fff;'>" +
            "<h3 style='color:#4f46e5;margin-top:0;'>You're registered! 🎉</h3>" +
            "<p>Dear <strong>" + fullName + "</strong>,</p>" +
            "<p style='color:#475569;'>Your registration for <strong>" + inv.getEventTitle() + "</strong> has been confirmed.</p>" +

            "<div style='background:#f8f7ff;border-radius:10px;padding:16px 20px;margin:20px 0;'>" +
            "<table style='width:100%;border-collapse:collapse;font-size:0.88rem;'>" +
            "<tr><td style='padding:5px 0;color:#64748b;width:40%'>Event</td><td style='font-weight:600;'>" + inv.getEventTitle() + "</td></tr>" +
            (eventDate.isBlank() ? "" : "<tr><td style='padding:5px 0;color:#64748b;'>Date</td><td style='font-weight:600;'>" + eventDate + "</td></tr>") +
            (venue.isBlank()     ? "" : "<tr><td style='padding:5px 0;color:#64748b;'>Venue</td><td style='font-weight:600;'>" + venue + "</td></tr>") +
            "<tr><td colspan='2'><hr style='border:none;border-top:1px solid #e0e7ff;margin:8px 0;'></td></tr>" +
            "<tr><td style='padding:5px 0;color:#64748b;'>Adult (18+)</td><td>" + safeInt(reg.getAttendeesAdult()) + " person(s)" + price(inv.getTicketPriceAdult()) + "</td></tr>" +
            "<tr><td style='padding:5px 0;color:#64748b;'>Youth (8–18)</td><td>" + safeInt(reg.getAttendeesYouth()) + " person(s)" + price(inv.getTicketPriceYouth()) + "</td></tr>" +
            "<tr><td style='padding:5px 0;color:#64748b;'>Under 8</td><td>" + safeInt(reg.getAttendeesUnder8()) + " person(s)" + price(inv.getTicketPriceUnder8()) + "</td></tr>" +
            (reg.getDonationAmount() != null && reg.getDonationAmount().compareTo(BigDecimal.ZERO) > 0
                ? "<tr><td style='padding:5px 0;color:#64748b;'>Donation</td><td>£" + reg.getDonationAmount().toPlainString() + "</td></tr>" : "") +
            "<tr><td colspan='2'><hr style='border:none;border-top:1px solid #e0e7ff;margin:8px 0;'></td></tr>" +
            "<tr><td style='padding:5px 0;color:#64748b;font-weight:700;'>Total</td><td style='font-weight:800;font-size:1rem;color:#4f46e5;'>" + totalStr + "</td></tr>" +
            "<tr><td style='padding:5px 0;color:#64748b;'>Ref</td><td style='font-family:monospace;font-size:0.82rem;'>REG-" + reg.getId() + "</td></tr>" +
            "</table></div>" +

            "<p style='color:#475569;font-size:0.88rem;'>Please present the QR code below at the event for check-in:</p>" +
            "<div style='text-align:center;margin:20px 0;'>" +
            "<img src='cid:qrcode' alt='Registration QR Code' style='width:200px;height:200px;border-radius:10px;border:4px solid #eef2ff;'>" +
            "</div>" +

            "<div style='background:#eef2ff;border-radius:10px;padding:14px 18px;text-align:center;margin-bottom:20px;'>" +
            "<p style='margin:0;font-size:0.82rem;color:#4f46e5;'>View event details &amp; share:</p>" +
            "<a href='" + publicUrl + "' style='color:#4f46e5;font-weight:700;font-size:0.88rem;'>" + publicUrl + "</a>" +
            "</div>" +

            "<p style='color:#94a3b8;font-size:0.78rem;margin-bottom:0;'>This is an automated confirmation from " + orgName + ". Please do not reply to this email.</p>" +
            "</div></div></body></html>";

        h.setText(body, true);
        h.addInline("qrcode", new org.springframework.core.io.ByteArrayResource(qrBytes), "image/png");
        mailSender.send(msg);
        log.info("Registration confirmation sent to {} for event: {}", reg.getEmail(), inv.getEventTitle());
    }

    private int safeInt(Integer v) { return v != null ? v : 0; }
    private String price(BigDecimal p) {
        if (p == null || p.compareTo(BigDecimal.ZERO) == 0) return " (Free)";
        return " @ £" + p.toPlainString();
    }
}
