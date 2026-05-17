package com.udjcs.finance;

import com.udjcs.invitation.InvitationRegistration;
import com.udjcs.invitation.InvitationRegistrationRepository;
import com.udjcs.member.MemberRepository;
import com.udjcs.organization.OrganizationRepository;
import com.udjcs.payment.Payment;
import com.udjcs.payment.PaymentRepository;
import com.udjcs.payment.PaymentService;
import com.udjcs.supportive.SupportiveOrganizationRepository;
import com.udjcs.ticket.EventTicket;
import com.udjcs.ticket.EventTicketRepository;
import jakarta.validation.Valid;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/finance")
public class FinanceController {

    private final SponsorDonationRepository sponsorRepo;
    private final PaymentRepository paymentRepo;
    private final EventTicketRepository ticketRepo;
    private final InvitationRegistrationRepository invRegRepo;
    private final SupportiveOrganizationRepository supportiveOrgRepo;
    private final MemberRepository memberRepo;
    private final PaymentInstalmentRepository instalmentRepo;
    private final OrganizationRepository orgRepo;

    public FinanceController(SponsorDonationRepository sponsorRepo,
                              PaymentRepository paymentRepo,
                              EventTicketRepository ticketRepo,
                              InvitationRegistrationRepository invRegRepo,
                              SupportiveOrganizationRepository supportiveOrgRepo,
                              MemberRepository memberRepo,
                              PaymentInstalmentRepository instalmentRepo,
                              OrganizationRepository orgRepo) {
        this.sponsorRepo       = sponsorRepo;
        this.paymentRepo       = paymentRepo;
        this.ticketRepo        = ticketRepo;
        this.invRegRepo        = invRegRepo;
        this.supportiveOrgRepo = supportiveOrgRepo;
        this.memberRepo        = memberRepo;
        this.instalmentRepo    = instalmentRepo;
        this.orgRepo           = orgRepo;
    }

    // ── Unified ledger ──────────────────────────────────────────────────────

    @GetMapping
    public String list(@RequestParam(required = false) String source, Model model) {
        List<PaymentRecord> all = buildLedger();

        if (source != null && !source.isBlank()) {
            all = all.stream().filter(r -> source.equals(r.getSource())).collect(Collectors.toList());
        }

        BigDecimal totalCommitted = all.stream().map(r -> r.getCommittedAmount() != null ? r.getCommittedAmount() : BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalDonated   = all.stream().map(r -> r.getDonatedAmount()   != null ? r.getDonatedAmount()   : BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalPending   = all.stream().map(r -> r.getPendingAmount()   != null ? r.getPendingAmount()   : BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("records", all);
        model.addAttribute("selectedSource", source);
        model.addAttribute("totalCommitted", totalCommitted);
        model.addAttribute("totalDonated", totalDonated);
        model.addAttribute("totalPending", totalPending);
        model.addAttribute("totalCount", all.size());
        return "finance/list";
    }

    // ── Sponsor Donations CRUD ───────────────────────────────────────────────

    @GetMapping("/sponsors/new")
    public String newSponsor(Model model) {
        model.addAttribute("item", new SponsorDonation());
        return "finance/sponsor-form";
    }

    @PostMapping("/sponsors")
    public String createSponsor(@ModelAttribute("item") @Valid SponsorDonation s,
                                 BindingResult result, RedirectAttributes attrs) {
        if (result.hasErrors()) return "finance/sponsor-form";
        sponsorRepo.save(s);
        attrs.addFlashAttribute("success", "Sponsor donation saved.");
        return "redirect:/finance";
    }

    @GetMapping("/sponsors/{id}/edit")
    public String editSponsor(@PathVariable Long id, Model model) {
        model.addAttribute("item", sponsorRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Not found: " + id)));
        model.addAttribute("instalments", instalmentRepo.findBySourceTypeAndSourceIdOrderByPaymentDateAsc("Sponsor", id));
        model.addAttribute("newInstalment", new PaymentInstalment());
        return "finance/sponsor-form";
    }

    @PostMapping("/sponsors/{id}/pay")
    public String addSponsorInstalment(@PathVariable Long id,
                                        @RequestParam BigDecimal amount,
                                        @RequestParam String paymentDate,
                                        @RequestParam String paymentMode,
                                        @RequestParam(required = false) String notes,
                                        RedirectAttributes attrs) {
        SponsorDonation sponsor = sponsorRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Not found: " + id));
        PaymentInstalment inst = new PaymentInstalment();
        inst.setSourceType("Sponsor");
        inst.setSourceId(id);
        inst.setAmount(amount);
        inst.setPaymentDate(java.time.LocalDate.parse(paymentDate));
        inst.setPaymentMode(paymentMode);
        inst.setNotes(notes);
        instalmentRepo.save(inst);
        BigDecimal totalPaid = instalmentRepo.sumBySourceTypeAndSourceId("Sponsor", id);
        sponsor.setDonatedAmount(totalPaid);
        if (sponsor.getCommittedAmount() != null && totalPaid.compareTo(sponsor.getCommittedAmount()) >= 0)
            sponsor.setSource("Sponsors");
        sponsorRepo.save(sponsor);
        attrs.addFlashAttribute("success", "Payment of £" + amount + " recorded.");
        return "redirect:/finance/sponsors/" + id + "/edit";
    }

    @PostMapping("/sponsors/{id}")
    public String updateSponsor(@PathVariable Long id,
                                 @ModelAttribute("item") @Valid SponsorDonation s,
                                 BindingResult result, RedirectAttributes attrs) {
        if (result.hasErrors()) return "finance/sponsor-form";
        s.setId(id);
        sponsorRepo.save(s);
        attrs.addFlashAttribute("success", "Sponsor donation updated.");
        return "redirect:/finance";
    }

    @PostMapping("/sponsors/{id}/delete")
    public String deleteSponsor(@PathVariable Long id, RedirectAttributes attrs) {
        sponsorRepo.deleteById(id);
        attrs.addFlashAttribute("success", "Sponsor donation deleted.");
        return "redirect:/finance";
    }

    // ── Organisation/Member Donations CRUD ──────────────────────────────────

    @GetMapping("/org-donations/new")
    public String newOrgDonation(@RequestParam(required = false) String type, Model model) {
        model.addAttribute("item", new Payment());
        model.addAttribute("supportiveOrgs", supportiveOrgRepo.findAll(Sort.by("name")));
        model.addAttribute("members", memberRepo.findAll(Sort.by("firstName", "lastName")));
        model.addAttribute("donorType", "member".equals(type) ? "member" : "org");
        return "finance/org-donation-form";
    }

    @PostMapping("/org-donations")
    public String createOrgDonation(@ModelAttribute("item") @Valid Payment p,
                                     BindingResult result, Model model,
                                     RedirectAttributes attrs) {
        if (result.hasErrors()) {
            model.addAttribute("supportiveOrgs", supportiveOrgRepo.findAll(Sort.by("name")));
            model.addAttribute("members", memberRepo.findAll(Sort.by("firstName", "lastName")));
            model.addAttribute("donorType", p.getMemberId() != null ? "member" : "org");
            return "finance/org-donation-form";
        }
        if (p.getOrganizationId() != null) {
            supportiveOrgRepo.findById(p.getOrganizationId()).ifPresent(p::setSupportiveOrganization);
        }
        if (p.getMemberId() != null) {
            memberRepo.findById(p.getMemberId()).ifPresent(p::setMember);
        }
        paymentRepo.save(p);
        attrs.addFlashAttribute("success", "Donation saved.");
        return "redirect:/finance";
    }

    @GetMapping("/org-donations/{id}/edit")
    public String editOrgDonation(@PathVariable Long id, Model model) {
        Payment p = paymentRepo.findByIdWithDetails(id)
                .orElseThrow(() -> new IllegalArgumentException("Not found: " + id));
        model.addAttribute("item", p);
        model.addAttribute("supportiveOrgs", supportiveOrgRepo.findAll(Sort.by("name")));
        model.addAttribute("members", memberRepo.findAll(Sort.by("firstName", "lastName")));
        model.addAttribute("donorType", p.getMember() != null ? "member" : "org");
        model.addAttribute("instalments", instalmentRepo.findBySourceTypeAndSourceIdOrderByPaymentDateAsc("OrgMember", id));
        model.addAttribute("newInstalment", new PaymentInstalment());
        return "finance/org-donation-form";
    }

    @PostMapping("/org-donations/{id}/pay")
    public String addOrgDonationInstalment(@PathVariable Long id,
                                            @RequestParam BigDecimal amount,
                                            @RequestParam String paymentDate,
                                            @RequestParam String paymentMode,
                                            @RequestParam(required = false) String notes,
                                            RedirectAttributes attrs) {
        Payment payment = paymentRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Not found: " + id));
        PaymentInstalment inst = new PaymentInstalment();
        inst.setSourceType("OrgMember");
        inst.setSourceId(id);
        inst.setAmount(amount);
        inst.setPaymentDate(java.time.LocalDate.parse(paymentDate));
        inst.setPaymentMode(paymentMode);
        inst.setNotes(notes);
        instalmentRepo.save(inst);
        BigDecimal totalPaid = instalmentRepo.sumBySourceTypeAndSourceId("OrgMember", id);
        payment.setAmount(totalPaid);
        if (payment.getCommittedAmount() != null && totalPaid.compareTo(payment.getCommittedAmount()) >= 0)
            payment.setStatus("Done");
        else if (totalPaid.compareTo(BigDecimal.ZERO) > 0)
            payment.setStatus("Partial");
        paymentRepo.save(payment);
        attrs.addFlashAttribute("success", "Payment of £" + amount + " recorded.");
        return "redirect:/finance/org-donations/" + id + "/edit";
    }

    @PostMapping("/instalments/{id}/delete")
    public String deleteInstalment(@PathVariable Long id, RedirectAttributes attrs) {
        PaymentInstalment inst = instalmentRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Not found: " + id));
        String sourceType = inst.getSourceType();
        Long sourceId = inst.getSourceId();
        instalmentRepo.deleteById(id);
        BigDecimal totalPaid = instalmentRepo.sumBySourceTypeAndSourceId(sourceType, sourceId);
        if ("Sponsor".equals(sourceType)) {
            SponsorDonation s = sponsorRepo.findById(sourceId).orElse(null);
            if (s != null) { s.setDonatedAmount(totalPaid); sponsorRepo.save(s); }
            attrs.addFlashAttribute("success", "Instalment removed.");
            return "redirect:/finance/sponsors/" + sourceId + "/edit";
        } else if ("Ticket".equals(sourceType)) {
            EventTicket t = ticketRepo.findById(sourceId).orElse(null);
            if (t != null) {
                t.setReceivedAmount(totalPaid.intValue());
                int committed = t.getCommittedAmount() != null ? t.getCommittedAmount() : (t.getTotalAmount() != null ? t.getTotalAmount() : 0);
                t.setStatus(totalPaid.intValue() >= committed ? "Accepted" : "Pending");
                ticketRepo.save(t);
            }
            attrs.addFlashAttribute("success", "Instalment removed.");
            return "redirect:/finance/tickets/" + sourceId + "/edit";
        } else {
            Payment p = paymentRepo.findById(sourceId).orElse(null);
            if (p != null) {
                p.setAmount(totalPaid);
                if (p.getCommittedAmount() != null && totalPaid.compareTo(p.getCommittedAmount()) >= 0) p.setStatus("Done");
                else if (totalPaid.compareTo(BigDecimal.ZERO) > 0) p.setStatus("Partial");
                else p.setStatus("Pending");
                paymentRepo.save(p);
            }
            attrs.addFlashAttribute("success", "Instalment removed.");
            return "redirect:/finance/org-donations/" + sourceId + "/edit";
        }
    }

    @PostMapping("/org-donations/{id}")
    public String updateOrgDonation(@PathVariable Long id,
                                     @ModelAttribute("item") @Valid Payment p,
                                     BindingResult result, Model model,
                                     RedirectAttributes attrs) {
        if (result.hasErrors()) {
            model.addAttribute("supportiveOrgs", supportiveOrgRepo.findAll(Sort.by("name")));
            model.addAttribute("members", memberRepo.findAll(Sort.by("firstName", "lastName")));
            model.addAttribute("donorType", p.getMemberId() != null ? "member" : "org");
            return "finance/org-donation-form";
        }
        p.setId(id);
        if (p.getOrganizationId() != null) {
            supportiveOrgRepo.findById(p.getOrganizationId()).ifPresent(p::setSupportiveOrganization);
        }
        if (p.getMemberId() != null) {
            memberRepo.findById(p.getMemberId()).ifPresent(p::setMember);
        }
        paymentRepo.save(p);
        attrs.addFlashAttribute("success", "Donation updated.");
        return "redirect:/finance";
    }

    // ── Event Ticket — edit payment details ─────────────────────────────────

    @GetMapping("/tickets/{id}/edit")
    public String editTicket(@PathVariable Long id, Model model) {
        model.addAttribute("item", ticketRepo.findByIdWithDetails(id)
                .orElseThrow(() -> new IllegalArgumentException("Not found: " + id)));
        model.addAttribute("instalments", instalmentRepo.findBySourceTypeAndSourceIdOrderByPaymentDateAsc("Ticket", id));
        return "finance/ticket-payment-form";
    }

    @PostMapping("/tickets/{id}")
    public String updateTicket(@PathVariable Long id,
                                @RequestParam(required = false) String paymentMode,
                                @RequestParam(required = false) String paymentDate,
                                @RequestParam(required = false) Integer committedAmount,
                                @RequestParam(required = false) Integer receivedAmount,
                                @RequestParam(required = false) String status,
                                RedirectAttributes attrs) {
        EventTicket t = ticketRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Not found: " + id));
        t.setPaymentMode(paymentMode);
        if (paymentDate != null && !paymentDate.isBlank())
            t.setPaymentDate(java.time.LocalDate.parse(paymentDate));
        if (committedAmount != null) t.setCommittedAmount(committedAmount);
        if (receivedAmount  != null) t.setReceivedAmount(receivedAmount);
        if (status != null && !status.isBlank()) t.setStatus(status);
        ticketRepo.save(t);
        attrs.addFlashAttribute("success", "Ticket payment details updated.");
        return "redirect:/finance";
    }

    @PostMapping("/tickets/{id}/pay")
    public String addTicketInstalment(@PathVariable Long id,
                                       @RequestParam java.math.BigDecimal amount,
                                       @RequestParam String paymentDate,
                                       @RequestParam String paymentMode,
                                       @RequestParam(required = false) String notes,
                                       RedirectAttributes attrs) {
        EventTicket t = ticketRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Not found: " + id));
        PaymentInstalment inst = new PaymentInstalment();
        inst.setSourceType("Ticket");
        inst.setSourceId(id);
        inst.setAmount(amount);
        inst.setPaymentDate(java.time.LocalDate.parse(paymentDate));
        inst.setPaymentMode(paymentMode);
        inst.setNotes(notes);
        instalmentRepo.save(inst);
        java.math.BigDecimal totalPaid = instalmentRepo.sumBySourceTypeAndSourceId("Ticket", id);
        t.setReceivedAmount(totalPaid.intValue());
        int committed = t.getCommittedAmount() != null ? t.getCommittedAmount() : (t.getTotalAmount() != null ? t.getTotalAmount() : 0);
        t.setStatus(totalPaid.intValue() >= committed ? "Accepted" : "Pending");
        ticketRepo.save(t);
        attrs.addFlashAttribute("success", "Payment of £" + amount + " recorded.");
        return "redirect:/finance/tickets/" + id + "/edit";
    }

    // ── Invitation Registration — edit payment details ───────────────────────

    @GetMapping("/inv-registrations/{id}/edit")
    public String editInvReg(@PathVariable Long id, Model model) {
        model.addAttribute("item", invRegRepo.findByIdWithInvitation(id)
                .orElseThrow(() -> new IllegalArgumentException("Not found: " + id)));
        return "finance/inv-reg-payment-form";
    }

    @PostMapping("/inv-registrations/{id}")
    public String updateInvReg(@PathVariable Long id,
                                @RequestParam(required = false) String paymentMode,
                                @RequestParam(required = false) String paymentDate,
                                RedirectAttributes attrs) {
        InvitationRegistration r = invRegRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Not found: " + id));
        r.setPaymentMode(paymentMode);
        if (paymentDate != null && !paymentDate.isBlank())
            r.setPaymentDate(java.time.LocalDate.parse(paymentDate));
        invRegRepo.save(r);
        attrs.addFlashAttribute("success", "Invitation registration payment updated.");
        return "redirect:/finance";
    }

    // ── Invoices ─────────────────────────────────────────────────────────────

    @GetMapping("/sponsors/{id}/invoice")
    public String sponsorInvoice(@PathVariable Long id, Model model) {
        SponsorDonation s = sponsorRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Not found: " + id));
        java.util.List<PaymentInstalment> instalments =
                instalmentRepo.findBySourceTypeAndSourceIdOrderByPaymentDateAsc("Sponsor", id);
        model.addAttribute("invoiceNumber", String.format("DON-%d-%04d", java.time.LocalDate.now().getYear(), s.getId()));
        model.addAttribute("invoiceDate", java.time.LocalDate.now());
        model.addAttribute("donorName", s.getSponsorName() != null ? s.getSponsorName() : s.getSponsorOrgName());
        model.addAttribute("donorOrg", s.getSponsorOrgName());
        model.addAttribute("donorType", "Sponsor Organisation");
        model.addAttribute("committedAmount", s.getCommittedAmount());
        model.addAttribute("paidAmount", s.getDonatedAmount());
        model.addAttribute("pendingAmount", s.getPendingAmount());
        model.addAttribute("status", instalments.isEmpty() ? (s.getPaymentDate() != null ? "Received" : "Pending") : "Instalment");
        model.addAttribute("instalments", instalments);
        model.addAttribute("directPaymentMode", s.getPaymentMode());
        model.addAttribute("directPaymentDate", s.getPaymentDate());
        model.addAttribute("remark", s.getRemark());
        model.addAttribute("org", orgRepo.findAll().stream().findFirst().orElse(null));
        return "finance/invoice";
    }

    @GetMapping("/org-donations/{id}/invoice")
    public String orgDonationInvoice(@PathVariable Long id, Model model) {
        Payment p = paymentRepo.findByIdWithDetails(id)
                .orElseThrow(() -> new IllegalArgumentException("Not found: " + id));
        java.util.List<PaymentInstalment> instalments =
                instalmentRepo.findBySourceTypeAndSourceIdOrderByPaymentDateAsc("OrgMember", id);
        String donorName = p.getMember() != null
                ? p.getMember().getFirstName() + " " + p.getMember().getLastName()
                : (p.getSupportiveOrganization() != null ? p.getSupportiveOrganization().getName() : "—");
        String donorOrg = p.getSupportiveOrganization() != null ? p.getSupportiveOrganization().getName() : null;
        java.math.BigDecimal pending = (p.getCommittedAmount() != null && p.getAmount() != null)
                ? p.getCommittedAmount().subtract(p.getAmount()) : java.math.BigDecimal.ZERO;
        model.addAttribute("invoiceNumber", String.format("DON-%d-%04d", java.time.LocalDate.now().getYear(), p.getId()));
        model.addAttribute("invoiceDate", java.time.LocalDate.now());
        model.addAttribute("donorName", donorName);
        model.addAttribute("donorOrg", donorOrg);
        model.addAttribute("donorType", p.getMember() != null ? "Member" : "Organisation");
        model.addAttribute("committedAmount", p.getCommittedAmount());
        model.addAttribute("paidAmount", p.getAmount());
        model.addAttribute("pendingAmount", pending);
        model.addAttribute("status", p.getStatus());
        model.addAttribute("instalments", instalments);
        model.addAttribute("directPaymentMode", p.getPaymentMode());
        model.addAttribute("directPaymentDate", p.getPaymentDate());
        model.addAttribute("remark", p.getNotes());
        model.addAttribute("org", orgRepo.findAll().stream().findFirst().orElse(null));
        return "finance/invoice";
    }

    // ── Build unified ledger ─────────────────────────────────────────────────

    private List<PaymentRecord> buildLedger() {
        List<PaymentRecord> all = new ArrayList<>();

        sponsorRepo.findAll(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "paymentDate"))
                .forEach(s -> all.add(PaymentRecord.fromSponsor(s)));

        paymentRepo.findAllWithDetails()
                .forEach(p -> all.add(PaymentRecord.fromOrgDonation(p)));

        ticketRepo.findAllWithDetails()
                .forEach(t -> all.add(PaymentRecord.fromTicket(t)));

        invRegRepo.findAllWithInvitation()
                .forEach(r -> all.add(PaymentRecord.fromInvitationReg(r)));

        all.sort(Comparator.comparing(
                r -> r.getPaymentDate() != null ? r.getPaymentDate() : java.time.LocalDate.MIN,
                Comparator.reverseOrder()));
        return all;
    }
}
