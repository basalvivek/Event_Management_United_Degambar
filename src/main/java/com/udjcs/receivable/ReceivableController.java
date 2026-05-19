package com.udjcs.receivable;

import com.udjcs.event.EventRepository;
import com.udjcs.finance.PaymentInstalment;
import com.udjcs.finance.PaymentInstalmentRepository;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/receivable")
public class ReceivableController {

    private final ReceivableTransactionRepository repo;
    private final PaymentInstalmentRepository instalmentRepo;
    private final EventRepository eventRepo;

    public ReceivableController(ReceivableTransactionRepository repo,
                                 PaymentInstalmentRepository instalmentRepo,
                                 EventRepository eventRepo) {
        this.repo          = repo;
        this.instalmentRepo = instalmentRepo;
        this.eventRepo     = eventRepo;
    }

    @GetMapping
    public String list(Model model) {
        List<ReceivableTransaction> items = repo.findAllOrdered();

        // Per-transaction received amount
        java.util.Map<Long, BigDecimal> paidMap = new java.util.LinkedHashMap<>();
        for (ReceivableTransaction r : items) {
            BigDecimal instSum = instalmentRepo.sumBySourceTypeAndSourceId("Receivable", r.getId());
            BigDecimal initial = r.getReceivedAmount() != null ? r.getReceivedAmount() : BigDecimal.ZERO;
            paidMap.put(r.getId(), initial.add(instSum));
        }

        BigDecimal grandTotal    = items.stream().map(r -> r.getTotalAmount() != null ? r.getTotalAmount() : BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal grandReceived = paidMap.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);

        // Two-level grouping: event → org name → items
        java.util.LinkedHashMap<String, java.util.LinkedHashMap<String, List<ReceivableTransaction>>> groups = new java.util.LinkedHashMap<>();
        items.stream()
            .sorted(java.util.Comparator.comparing(r -> r.getEvent() != null ? r.getEvent().getEventName().toLowerCase() : "zzz"))
            .forEach(r -> {
                String evKey  = r.getEvent() != null ? r.getEvent().getEventName() : "— No Event —";
                String orgKey = (r.getOrganisationName() != null && !r.getOrganisationName().isBlank())
                        ? r.getOrganisationName() : (r.getReceivedFrom() != null ? r.getReceivedFrom() : "— Unknown —");
                groups.computeIfAbsent(evKey, k -> new java.util.LinkedHashMap<>())
                      .computeIfAbsent(orgKey, k -> new java.util.ArrayList<>()).add(r);
            });
        // Within each org group: sort by receiptDate ASC then id ASC
        groups.values().forEach(orgMap -> orgMap.values().forEach(list ->
            list.sort(java.util.Comparator
                .comparing((ReceivableTransaction r) -> r.getReceiptDate() != null ? r.getReceiptDate() : java.time.LocalDate.MAX)
                .thenComparingLong(r -> r.getId() != null ? r.getId() : Long.MAX_VALUE))));

        java.util.Map<String, BigDecimal> eventTotals    = new java.util.LinkedHashMap<>();
        java.util.Map<String, BigDecimal> eventReceived  = new java.util.LinkedHashMap<>();
        java.util.Map<String, java.util.Map<String, BigDecimal>> orgTotals   = new java.util.LinkedHashMap<>();
        java.util.Map<String, java.util.Map<String, BigDecimal>> orgReceived = new java.util.LinkedHashMap<>();
        groups.forEach((evName, orgMap) -> {
            java.util.Map<String, BigDecimal> ot = new java.util.LinkedHashMap<>();
            java.util.Map<String, BigDecimal> or = new java.util.LinkedHashMap<>();
            orgMap.forEach((org, list) -> {
                ot.put(org, list.stream().map(r -> r.getTotalAmount() != null ? r.getTotalAmount() : BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add));
                or.put(org, list.stream().map(r -> paidMap.getOrDefault(r.getId(), BigDecimal.ZERO)).reduce(BigDecimal.ZERO, BigDecimal::add));
            });
            orgTotals.put(evName, ot);
            orgReceived.put(evName, or);
            eventTotals.put(evName, ot.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add));
            eventReceived.put(evName, or.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add));
        });

        model.addAttribute("items", items);
        model.addAttribute("groups", groups);
        model.addAttribute("eventTotals", eventTotals);
        model.addAttribute("eventReceived", eventReceived);
        model.addAttribute("orgTotals", orgTotals);
        model.addAttribute("orgReceived", orgReceived);
        model.addAttribute("paidMap", paidMap);
        model.addAttribute("grandTotal", grandTotal);
        model.addAttribute("grandReceived", grandReceived);
        model.addAttribute("grandBalance", grandTotal.subtract(grandReceived));
        return "receivable/list";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("item", new ReceivableTransaction());
        model.addAttribute("events", eventRepo.findAll(org.springframework.data.domain.Sort.by("eventName")));
        model.addAttribute("locked", false);
        return "receivable/form";
    }

    @PostMapping
    public String create(@ModelAttribute("item") @Valid ReceivableTransaction r,
                         BindingResult result, Model model, RedirectAttributes attrs) {
        if (result.hasErrors()) {
            model.addAttribute("events", eventRepo.findAll(org.springframework.data.domain.Sort.by("eventName")));
            model.addAttribute("locked", false);
            return "receivable/form";
        }
        wireEvent(r);
        autoName(r);
        computeStatus(r, BigDecimal.ZERO);
        repo.save(r);
        attrs.addFlashAttribute("success", "Receivable transaction saved.");
        return "redirect:/receivable";
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model) {
        ReceivableTransaction r = repo.findByIdWithEvent(id)
                .orElseThrow(() -> new IllegalArgumentException("Not found: " + id));
        if (r.getEvent() != null) r.setEventId(r.getEvent().getId());
        model.addAttribute("item", r);
        model.addAttribute("events", eventRepo.findAll(org.springframework.data.domain.Sort.by("eventName")));
        List<PaymentInstalment> instalments = instalmentRepo
                .findBySourceTypeAndSourceIdOrderByPaymentDateAsc("Receivable", id);
        BigDecimal instSum   = instalmentRepo.sumBySourceTypeAndSourceId("Receivable", id);
        BigDecimal totalRcvd = (r.getReceivedAmount() != null ? r.getReceivedAmount() : BigDecimal.ZERO).add(instSum);
        BigDecimal balance   = r.getTotalAmount() != null ? r.getTotalAmount().subtract(totalRcvd) : null;
        model.addAttribute("instalments", instalments);
        model.addAttribute("totalReceived", totalRcvd);
        model.addAttribute("balance", balance);
        model.addAttribute("locked", !instalments.isEmpty());
        return "receivable/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @ModelAttribute("item") @Valid ReceivableTransaction r,
                         BindingResult result, Model model, RedirectAttributes attrs) {
        if (result.hasErrors()) {
            java.util.List<com.udjcs.finance.PaymentInstalment> errinsts = instalmentRepo.findBySourceTypeAndSourceIdOrderByPaymentDateAsc("Receivable", id);
            model.addAttribute("events", eventRepo.findAll(org.springframework.data.domain.Sort.by("eventName")));
            model.addAttribute("instalments", errinsts);
            model.addAttribute("totalReceived", BigDecimal.ZERO);
            model.addAttribute("locked", !errinsts.isEmpty());
            return "receivable/form";
        }
        r.setId(id);
        wireEvent(r);
        autoName(r);
        BigDecimal instSum = instalmentRepo.sumBySourceTypeAndSourceId("Receivable", id);
        computeStatus(r, instSum);
        repo.save(r);
        attrs.addFlashAttribute("success", "Receivable transaction updated.");
        return "redirect:/receivable";
    }

    @Transactional
    @PostMapping("/{id}/receive")
    public String addReceipt(@PathVariable Long id,
                              @RequestParam BigDecimal amount,
                              @RequestParam String paymentDate,
                              @RequestParam String paymentMode,
                              @RequestParam(required = false) String notes,
                              RedirectAttributes attrs) {
        ReceivableTransaction r = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Not found: " + id));
        PaymentInstalment inst = new PaymentInstalment();
        inst.setSourceType("Receivable");
        inst.setSourceId(id);
        inst.setAmount(amount);
        inst.setPaymentDate(LocalDate.parse(paymentDate));
        inst.setPaymentMode(paymentMode);
        inst.setNotes(notes);
        instalmentRepo.save(inst);
        BigDecimal instSum = instalmentRepo.sumBySourceTypeAndSourceId("Receivable", id);
        computeStatus(r, instSum);
        repo.save(r);
        attrs.addFlashAttribute("success", "Receipt of £" + amount + " recorded.");
        return "redirect:/receivable/" + id + "/edit";
    }

    @Transactional
    @PostMapping("/instalments/{instId}/delete")
    public String deleteInstalment(@PathVariable Long instId, RedirectAttributes attrs) {
        PaymentInstalment inst = instalmentRepo.findById(instId)
                .orElseThrow(() -> new IllegalArgumentException("Not found: " + instId));
        Long rcvId = inst.getSourceId();
        instalmentRepo.deleteById(instId);
        ReceivableTransaction r = repo.findById(rcvId).orElse(null);
        if (r != null) {
            BigDecimal instSum = instalmentRepo.sumBySourceTypeAndSourceId("Receivable", rcvId);
            computeStatus(r, instSum);
            repo.save(r);
        }
        attrs.addFlashAttribute("success", "Receipt removed.");
        return "redirect:/receivable/" + rcvId + "/edit";
    }

    @Transactional
    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes attrs) {
        instalmentRepo.findBySourceTypeAndSourceIdOrderByPaymentDateAsc("Receivable", id)
                .forEach(i -> instalmentRepo.deleteById(i.getId()));
        repo.deleteById(id);
        attrs.addFlashAttribute("success", "Transaction deleted.");
        return "redirect:/receivable";
    }

    private void autoName(ReceivableTransaction r) {
        if ("TICKET_PAYMENT".equals(r.getIncomeType())
                && (r.getOrganisationName() == null || r.getOrganisationName().isBlank())) {
            r.setOrganisationName("Self");
        }
        if (r.getName() == null || r.getName().isBlank()) {
            String name = r.getOrganisationName();
            if (name == null || name.isBlank()) name = r.getReceivedFrom();
            if (name == null || name.isBlank()) name = r.getIncomeType();
            r.setName(name);
        }
    }

    private void wireEvent(ReceivableTransaction r) {
        if (r.getEventId() != null) {
            eventRepo.findById(r.getEventId()).ifPresent(r::setEvent);
        } else {
            r.setEvent(null);
        }
    }

    private void computeStatus(ReceivableTransaction r, BigDecimal instSum) {
        if (r.getTotalAmount() == null) return;
        BigDecimal received   = r.getReceivedAmount() != null ? r.getReceivedAmount() : BigDecimal.ZERO;
        BigDecimal totalRcvd  = received.add(instSum != null ? instSum : BigDecimal.ZERO);
        if (totalRcvd.compareTo(r.getTotalAmount()) >= 0)   r.setStatus("RECEIVED");
        else if (totalRcvd.compareTo(BigDecimal.ZERO) > 0)  r.setStatus("PARTIAL");
        else                                                  r.setStatus("PENDING");
    }
}
