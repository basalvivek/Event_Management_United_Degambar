package com.udjcs.receivable;

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

    public ReceivableController(ReceivableTransactionRepository repo,
                                 PaymentInstalmentRepository instalmentRepo) {
        this.repo          = repo;
        this.instalmentRepo = instalmentRepo;
    }

    @GetMapping
    public String list(Model model) {
        List<ReceivableTransaction> items = repo.findAllOrdered();
        BigDecimal grandTotal = items.stream()
                .map(r -> r.getTotalAmount() != null ? r.getTotalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Compute indices where a new organisation group starts (for template grouping)
        java.util.Set<Integer> groupStarts = new java.util.HashSet<>();
        String prevOrg = null;
        for (int i = 0; i < items.size(); i++) {
            String org = items.get(i).getOrganisationName();
            String key = (org != null && !org.isBlank()) ? org : "";
            if (!key.equals(prevOrg == null ? "" : prevOrg)) {
                groupStarts.add(i);
                prevOrg = key;
            }
        }

        model.addAttribute("items", items);
        model.addAttribute("grandTotal", grandTotal);
        model.addAttribute("groupStarts", groupStarts);
        return "receivable/list";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("item", new ReceivableTransaction());
        model.addAttribute("locked", false);
        return "receivable/form";
    }

    @PostMapping
    public String create(@ModelAttribute("item") @Valid ReceivableTransaction r,
                         BindingResult result, Model model, RedirectAttributes attrs) {
        if (result.hasErrors()) { model.addAttribute("locked", false); return "receivable/form"; }
        computeStatus(r, BigDecimal.ZERO);
        repo.save(r);
        attrs.addFlashAttribute("success", "Receivable transaction saved.");
        return "redirect:/receivable";
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model) {
        ReceivableTransaction r = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Not found: " + id));
        model.addAttribute("item", r);
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
            model.addAttribute("instalments", errinsts);
            model.addAttribute("totalReceived", BigDecimal.ZERO);
            model.addAttribute("locked", !errinsts.isEmpty());
            return "receivable/form";
        }
        r.setId(id);
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

    private void computeStatus(ReceivableTransaction r, BigDecimal instSum) {
        if (r.getTotalAmount() == null) return;
        BigDecimal received   = r.getReceivedAmount() != null ? r.getReceivedAmount() : BigDecimal.ZERO;
        BigDecimal totalRcvd  = received.add(instSum != null ? instSum : BigDecimal.ZERO);
        if (totalRcvd.compareTo(r.getTotalAmount()) >= 0)   r.setStatus("RECEIVED");
        else if (totalRcvd.compareTo(BigDecimal.ZERO) > 0)  r.setStatus("PARTIAL");
        else                                                  r.setStatus("PENDING");
    }
}
