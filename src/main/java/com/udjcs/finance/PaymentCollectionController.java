package com.udjcs.finance;

import com.udjcs.payment.Payment;
import com.udjcs.payment.PaymentRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/finance/payment-collection")
public class PaymentCollectionController {

    private final SponsorDonationRepository sponsorRepo;
    private final PaymentRepository paymentRepo;
    private final PaymentReminderRepository reminderRepo;

    public PaymentCollectionController(SponsorDonationRepository sponsorRepo,
                                        PaymentRepository paymentRepo,
                                        PaymentReminderRepository reminderRepo) {
        this.sponsorRepo = sponsorRepo;
        this.paymentRepo = paymentRepo;
        this.reminderRepo = reminderRepo;
    }

    @GetMapping
    public String show(Model model) {
        List<PendingDonationRow> sponsorRows = buildSponsorRows();
        List<Payment> pendingPayments = paymentRepo.findPendingWithDetails();
        List<PendingDonationRow> orgRows = buildOrgRows(pendingPayments);
        List<PendingDonationRow> memberRows = buildMemberRows(pendingPayments);

        BigDecimal sponsorTotal = sum(sponsorRows);
        BigDecimal orgTotal     = sum(orgRows);
        BigDecimal memberTotal  = sum(memberRows);

        model.addAttribute("sponsorRows",  sponsorRows);
        model.addAttribute("orgRows",      orgRows);
        model.addAttribute("memberRows",   memberRows);
        model.addAttribute("sponsorTotal", sponsorTotal);
        model.addAttribute("orgTotal",     orgTotal);
        model.addAttribute("memberTotal",  memberTotal);
        model.addAttribute("grandTotal",   sponsorTotal.add(orgTotal).add(memberTotal));
        model.addAttribute("sponsorCommittedTotal", committed(sponsorRows));
        model.addAttribute("sponsorPaidTotal",      paid(sponsorRows));
        model.addAttribute("orgCommittedTotal",     committed(orgRows));
        model.addAttribute("orgPaidTotal",          paid(orgRows));
        model.addAttribute("memberCommittedTotal",  committed(memberRows));
        model.addAttribute("memberPaidTotal",       paid(memberRows));
        return "finance/payment-collection";
    }

    @PostMapping("/reminder")
    public String sendReminder(@RequestParam String sourceType,
                                @RequestParam Long sourceId,
                                RedirectAttributes attrs) {
        PaymentReminder r = new PaymentReminder();
        r.setSourceType(sourceType);
        r.setSourceId(sourceId);
        r.setReminderDate(LocalDate.now());
        reminderRepo.save(r);
        attrs.addFlashAttribute("success", "Reminder recorded successfully.");
        return "redirect:/finance/payment-collection";
    }

    private List<PendingDonationRow> buildSponsorRows() {
        return sponsorRepo.findPendingWithEvent().stream()
            .map(s -> new PendingDonationRow(
                "Sponsor", s.getId(),
                s.getEvent() != null ? s.getEvent().getEventName() : null,
                s.getSponsorOrgName(),
                s.getSponsorName(),
                s.getCommittedAmount(),
                s.getDonatedAmount(),
                reminderRepo.countBySourceTypeAndSourceId("Sponsor", s.getId())
            ))
            .collect(Collectors.toList());
    }

    private List<PendingDonationRow> buildOrgRows(List<Payment> payments) {
        return payments.stream()
            .filter(p -> p.getMember() == null && p.getSupportiveOrganization() != null)
            .map(p -> new PendingDonationRow(
                "OrgDonation", p.getId(),
                p.getEvent() != null ? p.getEvent().getEventName() : null,
                p.getSupportiveOrganization().getName(),
                null,
                p.getCommittedAmount(),
                p.getAmount(),
                reminderRepo.countBySourceTypeAndSourceId("OrgDonation", p.getId())
            ))
            .collect(Collectors.toList());
    }

    private List<PendingDonationRow> buildMemberRows(List<Payment> payments) {
        return payments.stream()
            .filter(p -> p.getMember() != null)
            .map(p -> new PendingDonationRow(
                "MemberDonation", p.getId(),
                p.getEvent() != null ? p.getEvent().getEventName() : null,
                p.getMember().getFirstName() + " " + p.getMember().getLastName(),
                null,
                p.getCommittedAmount(),
                p.getAmount(),
                reminderRepo.countBySourceTypeAndSourceId("MemberDonation", p.getId())
            ))
            .collect(Collectors.toList());
    }

    private static BigDecimal sum(List<PendingDonationRow> rows) {
        return rows.stream().map(PendingDonationRow::getPendingAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal committed(List<PendingDonationRow> rows) {
        return rows.stream().map(PendingDonationRow::getCommittedAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal paid(List<PendingDonationRow> rows) {
        return rows.stream().map(PendingDonationRow::getPaidAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
