package com.udjcs.finance;

import com.udjcs.event.Event;
import com.udjcs.event.EventRepository;
import com.udjcs.invitation.InvitationRegistration;
import com.udjcs.invitation.InvitationRegistrationRepository;
import com.udjcs.member.MemberRepository;
import com.udjcs.organization.OrganizationRepository;
import com.udjcs.payment.Payment;
import com.udjcs.payment.PaymentRepository;
import com.udjcs.payment.PaymentService;
import com.udjcs.receivable.ReceivableTransaction;
import com.udjcs.receivable.ReceivableTransactionRepository;
import com.udjcs.supportive.SupportiveOrganizationRepository;
import com.udjcs.ticket.EventTicket;
import com.udjcs.ticket.EventTicketRepository;
import jakarta.transaction.Transactional;
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
    private final ReceivableTransactionRepository receivableRepo;
    private final EventRepository eventRepo;

    public FinanceController(SponsorDonationRepository sponsorRepo,
                              PaymentRepository paymentRepo,
                              EventTicketRepository ticketRepo,
                              InvitationRegistrationRepository invRegRepo,
                              SupportiveOrganizationRepository supportiveOrgRepo,
                              MemberRepository memberRepo,
                              PaymentInstalmentRepository instalmentRepo,
                              OrganizationRepository orgRepo,
                              ReceivableTransactionRepository receivableRepo,
                              EventRepository eventRepo) {
        this.sponsorRepo       = sponsorRepo;
        this.paymentRepo       = paymentRepo;
        this.ticketRepo        = ticketRepo;
        this.invRegRepo        = invRegRepo;
        this.supportiveOrgRepo = supportiveOrgRepo;
        this.memberRepo        = memberRepo;
        this.instalmentRepo    = instalmentRepo;
        this.orgRepo           = orgRepo;
        this.receivableRepo    = receivableRepo;
        this.eventRepo         = eventRepo;
    }

    private void autoReceivable(String incomeType, String name, String orgName,
                                 String person, java.math.BigDecimal amount,
                                 java.time.LocalDate date, String modeAndNotes, Event event) {
        ReceivableTransaction r = new ReceivableTransaction();
        r.setIncomeType(incomeType);
        r.setName(name);
        r.setOrganisationName(orgName);
        r.setReceivedFrom(person);
        r.setTotalAmount(amount);
        r.setReceivedAmount(amount);
        r.setReceiptDate(date);
        r.setStatus("RECEIVED");
        r.setNotes(modeAndNotes);
        r.setEvent(event);
        receivableRepo.save(r);
    }

    // ── Unified ledger ──────────────────────────────────────────────────────

    @GetMapping
    public String list(@RequestParam(required = false) String source, Model model) {
        List<PaymentRecord> all = buildLedger();

        if (source != null && !source.isBlank()) {
            all = all.stream().filter(r -> source.equals(r.getSource())).collect(Collectors.toList());
        }

        // Sort each record within same source by paymentDate ASC, then id ASC
        java.util.Comparator<PaymentRecord> payOrder = java.util.Comparator
            .comparing((PaymentRecord r) -> r.getPaymentDate() != null ? r.getPaymentDate() : java.time.LocalDate.MAX)
            .thenComparingLong(r -> r.getSourceId() != null ? r.getSourceId() : Long.MAX_VALUE);

        // Use donated (received) for all totals; committed and pending computed after dedup
        BigDecimal totalDonated   = all.stream().map(r -> r.getDonatedAmount()   != null ? r.getDonatedAmount()   : BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add);

        // Two-level grouping: event → org name → items
        java.util.LinkedHashMap<String, java.util.LinkedHashMap<String, List<PaymentRecord>>> groups = new java.util.LinkedHashMap<>();
        all.forEach(r -> {
            String evKey  = r.getEventName() != null && !r.getEventName().isBlank() ? r.getEventName() : "— No Event —";
            String orgKey = (r.getOrgName() != null && !r.getOrgName().isBlank() && !r.getOrgName().equals("—"))
                    ? r.getOrgName() : (r.getPersonName() != null ? r.getPersonName() : "— Unknown —");
            groups.computeIfAbsent(evKey, k -> new java.util.LinkedHashMap<>())
                  .computeIfAbsent(orgKey, k -> new ArrayList<>()).add(r);
        });

        // Sort within each org group by paymentDate ASC
        groups.values().forEach(orgMap -> orgMap.values().forEach(list -> list.sort(payOrder)));

        java.util.Map<String, BigDecimal> eventTotals    = new java.util.LinkedHashMap<>();
        java.util.Map<String, java.util.Map<String, BigDecimal>> orgCommitted = new java.util.LinkedHashMap<>();
        java.util.Map<String, java.util.Map<String, BigDecimal>> orgReceived  = new java.util.LinkedHashMap<>();
        java.util.Map<String, java.util.Map<String, BigDecimal>> orgPending   = new java.util.LinkedHashMap<>();
        // First PaymentRecord per org group (for edit/print links on summary row)
        java.util.Map<String, java.util.Map<String, PaymentRecord>> orgFirstRecord = new java.util.LinkedHashMap<>();
        groups.forEach((evName, orgMap) -> {
            java.util.Map<String, BigDecimal> oc = new java.util.LinkedHashMap<>();
            java.util.Map<String, BigDecimal> or = new java.util.LinkedHashMap<>();
            java.util.Map<String, BigDecimal> op = new java.util.LinkedHashMap<>();
            java.util.Map<String, PaymentRecord> of = new java.util.LinkedHashMap<>();
            orgMap.forEach((org, list) -> {
                // committedAmount is the same on all rows of a group — take from first row
                BigDecimal committed = list.get(0).getCommittedAmount() != null ? list.get(0).getCommittedAmount() : BigDecimal.ZERO;
                BigDecimal received  = list.stream().map(r -> r.getDonatedAmount() != null ? r.getDonatedAmount() : BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add);
                oc.put(org, committed);
                or.put(org, received);
                op.put(org, committed.subtract(received).max(BigDecimal.ZERO));
                of.put(org, list.get(0));
            });
            orgCommitted.put(evName, oc);
            orgReceived.put(evName, or);
            orgPending.put(evName, op);
            orgFirstRecord.put(evName, of);
            eventTotals.put(evName, or.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add));
        });

        BigDecimal totalCommitted = orgCommitted.values().stream().flatMap(m -> m.values().stream()).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalPending   = orgPending.values().stream().flatMap(m -> m.values().stream()).reduce(BigDecimal.ZERO, BigDecimal::add);
        model.addAttribute("groups", groups);
        model.addAttribute("eventTotals", eventTotals);
        model.addAttribute("orgCommitted", orgCommitted);
        model.addAttribute("orgReceived", orgReceived);
        model.addAttribute("orgPending", orgPending);
        model.addAttribute("orgFirstRecord", orgFirstRecord);
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
        model.addAttribute("locked", false);
        model.addAttribute("individualSponsors", supportiveOrgRepo.findAllSponsors());
        model.addAttribute("events", eventRepo.findAll(org.springframework.data.domain.Sort.by("eventName")));
        return "finance/sponsor-form";
    }

    @Transactional
    @PostMapping("/sponsors")
    public String createSponsor(@ModelAttribute("item") @Valid SponsorDonation s,
                                 BindingResult result, Model model, RedirectAttributes attrs) {
        if (result.hasErrors()) { model.addAttribute("locked", false); model.addAttribute("individualSponsors", supportiveOrgRepo.findByOrganizationTypeOrderByNameAsc("Individual Sponsor")); model.addAttribute("events", eventRepo.findAll(org.springframework.data.domain.Sort.by("eventName"))); return "finance/sponsor-form"; }
        wireEvent(s);
        sponsorRepo.save(s);
        if (s.getDonatedAmount() != null && s.getDonatedAmount().compareTo(BigDecimal.ZERO) > 0) {
            PaymentInstalment initial = new PaymentInstalment();
            initial.setSourceType("Sponsor");
            initial.setSourceId(s.getId());
            initial.setAmount(s.getDonatedAmount());
            initial.setPaymentDate(s.getPaymentDate());
            initial.setPaymentMode(s.getPaymentMode());
            initial.setNotes("Initial payment");
            instalmentRepo.save(initial);
            String memo = s.getPaymentMode()
                    + (s.getRemark() != null && !s.getRemark().isBlank() ? " | " + s.getRemark() : "");
            autoReceivable("SPONSOR",
                    s.getSponsorOrgName() + " — Sponsor Donation",
                    s.getSponsorOrgName(), s.getSponsorName(),
                    s.getDonatedAmount(), s.getPaymentDate(), memo, s.getEvent());
        }
        attrs.addFlashAttribute("success", "Sponsor donation saved.");
        return "redirect:/finance";
    }

    @GetMapping("/sponsors/{id}/edit")
    public String editSponsor(@PathVariable Long id, Model model) {
        SponsorDonation s = sponsorRepo.findByIdWithEvent(id)
                .orElseThrow(() -> new IllegalArgumentException("Not found: " + id));
        if (s.getEvent() != null) s.setEventId(s.getEvent().getId());
        model.addAttribute("item", s);
        java.util.List<PaymentInstalment> insts = instalmentRepo.findBySourceTypeAndSourceIdOrderByPaymentDateAsc("Sponsor", id);
        model.addAttribute("instalments", insts);
        model.addAttribute("newInstalment", new PaymentInstalment());
        model.addAttribute("locked", !insts.isEmpty());
        model.addAttribute("individualSponsors", supportiveOrgRepo.findAllSponsors());
        model.addAttribute("events", eventRepo.findAll(org.springframework.data.domain.Sort.by("eventName")));
        return "finance/sponsor-form";
    }

    @Transactional
    @PostMapping("/sponsors/{id}/pay")
    public String addSponsorInstalment(@PathVariable Long id,
                                        @RequestParam BigDecimal amount,
                                        @RequestParam String paymentDate,
                                        @RequestParam String paymentMode,
                                        @RequestParam(required = false) String notes,
                                        RedirectAttributes attrs) {
        SponsorDonation sponsor = sponsorRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Not found: " + id));
        java.time.LocalDate date = java.time.LocalDate.parse(paymentDate);
        PaymentInstalment inst = new PaymentInstalment();
        inst.setSourceType("Sponsor");
        inst.setSourceId(id);
        inst.setAmount(amount);
        inst.setPaymentDate(date);
        inst.setPaymentMode(paymentMode);
        inst.setNotes(notes);
        instalmentRepo.save(inst);
        BigDecimal totalPaid = instalmentRepo.sumBySourceTypeAndSourceId("Sponsor", id);
        sponsor.setDonatedAmount(totalPaid);
        if (sponsor.getCommittedAmount() != null && totalPaid.compareTo(sponsor.getCommittedAmount()) >= 0)
            sponsor.setSource("Sponsors");
        sponsorRepo.save(sponsor);
        String memo = paymentMode + (notes != null && !notes.isBlank() ? " | " + notes : "");
        autoReceivable("SPONSOR",
                sponsor.getSponsorOrgName() + " — Sponsor Payment",
                sponsor.getSponsorOrgName(), sponsor.getSponsorName(),
                amount, date, memo, sponsor.getEvent());
        attrs.addFlashAttribute("success", "Payment of £" + amount + " recorded.");
        return "redirect:/finance/sponsors/" + id + "/edit";
    }

    @PostMapping("/sponsors/{id}")
    public String updateSponsor(@PathVariable Long id,
                                 @ModelAttribute("item") @Valid SponsorDonation s,
                                 BindingResult result, Model model, RedirectAttributes attrs) {
        if (result.hasErrors()) { model.addAttribute("locked", false); return "finance/sponsor-form"; }
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
        model.addAttribute("supportiveOrgs", supportiveOrgRepo.findByOrganizationTypeNotOrderByNameAsc("Individual Sponsor"));
        model.addAttribute("members", memberRepo.findAll(Sort.by("firstName", "lastName")));
        model.addAttribute("events", eventRepo.findAll(org.springframework.data.domain.Sort.by("eventName")));
        model.addAttribute("donorType", "member".equals(type) ? "member" : "org");
        model.addAttribute("locked", false);
        return "finance/org-donation-form";
    }

    @Transactional
    @PostMapping("/org-donations")
    public String createOrgDonation(@ModelAttribute("item") @Valid Payment p,
                                     BindingResult result, Model model,
                                     RedirectAttributes attrs) {
        if (result.hasErrors()) {
            model.addAttribute("supportiveOrgs", supportiveOrgRepo.findByOrganizationTypeNotOrderByNameAsc("Individual Sponsor"));
            model.addAttribute("members", memberRepo.findAll(Sort.by("firstName", "lastName")));
            model.addAttribute("donorType", p.getMemberId() != null ? "member" : "org");
            model.addAttribute("locked", false);
            return "finance/org-donation-form";
        }
        if (p.getOrganizationId() != null) {
            supportiveOrgRepo.findById(p.getOrganizationId()).ifPresent(p::setSupportiveOrganization);
        }
        if (p.getMemberId() != null) {
            memberRepo.findById(p.getMemberId()).ifPresent(p::setMember);
        }
        wireEvent(p);
        paymentRepo.save(p);
        if (p.getAmount() != null && p.getAmount().compareTo(BigDecimal.ZERO) > 0 && p.getPaymentDate() != null) {
            PaymentInstalment initial = new PaymentInstalment();
            initial.setSourceType("OrgMember");
            initial.setSourceId(p.getId());
            initial.setAmount(p.getAmount());
            initial.setPaymentDate(p.getPaymentDate());
            initial.setPaymentMode(p.getPaymentMode() != null ? p.getPaymentMode() : "");
            initial.setNotes("Initial payment");
            instalmentRepo.save(initial);
            boolean isMember = p.getMember() != null;
            String orgName  = p.getSupportiveOrganization() != null ? p.getSupportiveOrganization().getName() : null;
            String person   = isMember ? p.getMember().getFirstName() + " " + p.getMember().getLastName() : orgName;
            String incType  = isMember ? "MEMBER_DONATION" : "ORG_DONATION";
            String recName  = (orgName != null ? orgName : person) + " — Donation";
            String memo     = (p.getPaymentMode() != null ? p.getPaymentMode() : "")
                            + (p.getNotes() != null && !p.getNotes().isBlank() ? " | " + p.getNotes() : "");
            autoReceivable(incType, recName, orgName, person, p.getAmount(), p.getPaymentDate(), memo, p.getEvent());
        }
        attrs.addFlashAttribute("success", "Donation saved.");
        return "redirect:/finance";
    }

    @GetMapping("/org-donations/{id}/edit")
    public String editOrgDonation(@PathVariable Long id, Model model) {
        Payment p = paymentRepo.findByIdWithDetails(id)
                .orElseThrow(() -> new IllegalArgumentException("Not found: " + id));
        if (p.getEvent() != null) p.setEventId(p.getEvent().getId());
        java.util.List<PaymentInstalment> insts = instalmentRepo.findBySourceTypeAndSourceIdOrderByPaymentDateAsc("OrgMember", id);
        model.addAttribute("item", p);
        model.addAttribute("supportiveOrgs", supportiveOrgRepo.findByOrganizationTypeNotOrderByNameAsc("Individual Sponsor"));
        model.addAttribute("members", memberRepo.findAll(Sort.by("firstName", "lastName")));
        model.addAttribute("events", eventRepo.findAll(org.springframework.data.domain.Sort.by("eventName")));
        model.addAttribute("donorType", p.getMember() != null ? "member" : "org");
        model.addAttribute("instalments", insts);
        model.addAttribute("newInstalment", new PaymentInstalment());
        model.addAttribute("locked", !insts.isEmpty());
        return "finance/org-donation-form";
    }

    @Transactional
    @PostMapping("/org-donations/{id}/pay")
    public String addOrgDonationInstalment(@PathVariable Long id,
                                            @RequestParam BigDecimal amount,
                                            @RequestParam String paymentDate,
                                            @RequestParam String paymentMode,
                                            @RequestParam(required = false) String notes,
                                            RedirectAttributes attrs) {
        Payment payment = paymentRepo.findByIdWithDetails(id)
                .orElseThrow(() -> new IllegalArgumentException("Not found: " + id));
        java.time.LocalDate date = java.time.LocalDate.parse(paymentDate);
        PaymentInstalment inst = new PaymentInstalment();
        inst.setSourceType("OrgMember");
        inst.setSourceId(id);
        inst.setAmount(amount);
        inst.setPaymentDate(date);
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
        boolean isMember = payment.getMember() != null;
        String orgName  = payment.getSupportiveOrganization() != null ? payment.getSupportiveOrganization().getName() : null;
        String person   = isMember ? payment.getMember().getFirstName() + " " + payment.getMember().getLastName() : orgName;
        String incType  = isMember ? "MEMBER_DONATION" : "ORG_DONATION";
        String recName  = (orgName != null ? orgName : person) + " — Donation Payment";
        String memo     = paymentMode + (notes != null && !notes.isBlank() ? " | " + notes : "");
        autoReceivable(incType, recName, orgName, person, amount, date, memo, payment.getEvent());
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
            model.addAttribute("supportiveOrgs", supportiveOrgRepo.findByOrganizationTypeNotOrderByNameAsc("Individual Sponsor"));
            model.addAttribute("members", memberRepo.findAll(Sort.by("firstName", "lastName")));
            model.addAttribute("donorType", p.getMemberId() != null ? "member" : "org");
            model.addAttribute("locked", false);
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
        java.util.List<PaymentInstalment> insts = instalmentRepo.findBySourceTypeAndSourceIdOrderByPaymentDateAsc("Ticket", id);
        model.addAttribute("instalments", insts);
        model.addAttribute("locked", !insts.isEmpty());
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

    @Transactional
    @PostMapping("/tickets/{id}/pay")
    public String addTicketInstalment(@PathVariable Long id,
                                       @RequestParam java.math.BigDecimal amount,
                                       @RequestParam String paymentDate,
                                       @RequestParam String paymentMode,
                                       @RequestParam(required = false) String notes,
                                       RedirectAttributes attrs) {
        EventTicket t = ticketRepo.findByIdWithDetails(id)
                .orElseThrow(() -> new IllegalArgumentException("Not found: " + id));
        java.time.LocalDate date = java.time.LocalDate.parse(paymentDate);
        PaymentInstalment inst = new PaymentInstalment();
        inst.setSourceType("Ticket");
        inst.setSourceId(id);
        inst.setAmount(amount);
        inst.setPaymentDate(date);
        inst.setPaymentMode(paymentMode);
        inst.setNotes(notes);
        instalmentRepo.save(inst);
        java.math.BigDecimal totalPaid = instalmentRepo.sumBySourceTypeAndSourceId("Ticket", id);
        t.setReceivedAmount(totalPaid.intValue());
        int committed = t.getCommittedAmount() != null ? t.getCommittedAmount() : (t.getTotalAmount() != null ? t.getTotalAmount() : 0);
        t.setStatus(totalPaid.intValue() >= committed ? "Accepted" : "Pending");
        ticketRepo.save(t);
        String eventName  = t.getEvent()  != null ? t.getEvent().getEventName() : "Ticket";
        String memberName = t.getMember() != null ? t.getMember().getFirstName() + " " + t.getMember().getLastName() : null;
        String memo = paymentMode + (notes != null && !notes.isBlank() ? " | " + notes : "");
        autoReceivable("TICKET_PAYMENT", eventName + " — Ticket Payment",
                eventName, memberName, amount, date, memo, t.getEvent());
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

        // Sponsors — one row per instalment with running pending per row
        sponsorRepo.findAllWithEvent().forEach(s -> {
            List<PaymentInstalment> insts = instalmentRepo
                    .findBySourceTypeAndSourceIdOrderByPaymentDateAsc("Sponsor", s.getId());
            if (insts.isEmpty()) {
                all.add(PaymentRecord.fromSponsor(s));
            } else {
                BigDecimal committed = s.getCommittedAmount() != null ? s.getCommittedAmount() : BigDecimal.ZERO;
                BigDecimal[] running = {BigDecimal.ZERO};
                insts.forEach(inst -> {
                    running[0] = running[0].add(inst.getAmount());
                    all.add(PaymentRecord.fromSponsorInstalment(s, inst, committed.subtract(running[0])));
                });
            }
        });

        // Org/Member donations — one row per instalment with running pending
        paymentRepo.findAllWithDetails().forEach(p -> {
            List<PaymentInstalment> insts = instalmentRepo
                    .findBySourceTypeAndSourceIdOrderByPaymentDateAsc("OrgMember", p.getId());
            if (insts.isEmpty()) {
                all.add(PaymentRecord.fromOrgDonation(p));
            } else {
                BigDecimal committed = p.getCommittedAmount() != null ? p.getCommittedAmount() : BigDecimal.ZERO;
                BigDecimal[] running = {BigDecimal.ZERO};
                insts.forEach(inst -> {
                    running[0] = running[0].add(inst.getAmount());
                    all.add(PaymentRecord.fromOrgDonationInstalment(p, inst, committed.subtract(running[0])));
                });
            }
        });

        // Ticket payments — one row per instalment with running pending
        ticketRepo.findAllWithDetails().forEach(t -> {
            List<PaymentInstalment> insts = instalmentRepo
                    .findBySourceTypeAndSourceIdOrderByPaymentDateAsc("Ticket", t.getId());
            if (insts.isEmpty()) {
                all.add(PaymentRecord.fromTicket(t));
            } else {
                BigDecimal total     = t.getTotalAmount()    != null ? BigDecimal.valueOf(t.getTotalAmount())    : BigDecimal.ZERO;
                BigDecimal committed = t.getCommittedAmount() != null ? BigDecimal.valueOf(t.getCommittedAmount()) : total;
                BigDecimal[] running = {BigDecimal.ZERO};
                insts.forEach(inst -> {
                    running[0] = running[0].add(inst.getAmount());
                    all.add(PaymentRecord.fromTicketInstalment(t, inst, committed.subtract(running[0])));
                });
            }
        });

        // Invitations — always one row (no instalment concept)
        invRegRepo.findAllWithInvitation()
                .forEach(r -> all.add(PaymentRecord.fromInvitationReg(r)));

        all.sort((a, b) -> {
            String ka = eventGroupKey(a.getEventName());
            String kb = eventGroupKey(b.getEventName());
            int cmp = ka.compareToIgnoreCase(kb);
            if (cmp != 0) return cmp;
            java.time.LocalDate da = a.getPaymentDate() != null ? a.getPaymentDate() : java.time.LocalDate.MIN;
            java.time.LocalDate db = b.getPaymentDate() != null ? b.getPaymentDate() : java.time.LocalDate.MIN;
            return db.compareTo(da);
        });
        return all;
    }

    private static String eventGroupKey(String eventName) {
        return (eventName == null || eventName.isBlank()) ? "zzz" : eventName;
    }

    private static String groupKey(String orgName) {
        return (orgName == null || orgName.isBlank() || "—".equals(orgName)) ? "￿" : orgName;
    }

    private void wireEvent(SponsorDonation s) {
        if (s.getEventId() != null) eventRepo.findById(s.getEventId()).ifPresent(s::setEvent);
        else s.setEvent(null);
    }

    private void wireEvent(Payment p) {
        if (p.getEventId() != null) eventRepo.findById(p.getEventId()).ifPresent(p::setEvent);
        else p.setEvent(null);
    }
}
