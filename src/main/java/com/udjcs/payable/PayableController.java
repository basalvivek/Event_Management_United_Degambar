package com.udjcs.payable;

import com.udjcs.finance.PaymentInstalment;
import com.udjcs.finance.PaymentInstalmentRepository;
import com.udjcs.food.FoodRegistration;
import com.udjcs.food.FoodRegistrationRepository;
import com.udjcs.hall.HallRegistration;
import com.udjcs.hall.HallRegistrationRepository;
import jakarta.validation.Valid;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/payable")
public class PayableController {

    private final PayableTransactionRepository repo;
    private final PaymentInstalmentRepository instalmentRepo;
    private final HallRegistrationRepository hallRepo;
    private final FoodRegistrationRepository foodRepo;

    public PayableController(PayableTransactionRepository repo,
                              PaymentInstalmentRepository instalmentRepo,
                              HallRegistrationRepository hallRepo,
                              FoodRegistrationRepository foodRepo) {
        this.repo          = repo;
        this.instalmentRepo = instalmentRepo;
        this.hallRepo      = hallRepo;
        this.foodRepo      = foodRepo;
    }

    @GetMapping
    public String list(Model model) {
        List<PayableTransaction> items = repo.findAllOrdered();

        // Build map of real total-paid per payable (deposit + partial payments)
        java.util.Map<Long, BigDecimal> paidMap = new java.util.LinkedHashMap<>();
        for (PayableTransaction p : items) {
            BigDecimal instSum = instalmentRepo.sumBySourceTypeAndSourceId("Payable", p.getId());
            BigDecimal deposit = p.getInitialDeposit() != null ? p.getInitialDeposit() : BigDecimal.ZERO;
            paidMap.put(p.getId(), deposit.add(instSum));
        }

        BigDecimal grandTotal   = items.stream().map(p -> p.getTotalAmount() != null ? p.getTotalAmount() : BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal grandPaid    = paidMap.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        model.addAttribute("items", items);
        model.addAttribute("paidMap", paidMap);
        model.addAttribute("grandTotal", grandTotal);
        model.addAttribute("grandPaid", grandPaid);
        model.addAttribute("grandBalance", grandTotal.subtract(grandPaid));
        return "payable/list";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("item", new PayableTransaction());
        addFormData(model);
        return "payable/form";
    }

    @PostMapping
    public String create(@ModelAttribute("item") @Valid PayableTransaction p,
                         BindingResult result, Model model, RedirectAttributes attrs) {
        if (result.hasErrors()) { addFormData(model); return "payable/form"; }
        computeStatus(p, BigDecimal.ZERO);
        repo.save(p);
        attrs.addFlashAttribute("success", "Payable transaction saved.");
        return "redirect:/payable";
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model) {
        PayableTransaction p = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Not found: " + id));
        model.addAttribute("item", p);
        addFormData(model);
        List<PaymentInstalment> instalments = instalmentRepo
                .findBySourceTypeAndSourceIdOrderByPaymentDateAsc("Payable", id);
        BigDecimal instSum   = instalmentRepo.sumBySourceTypeAndSourceId("Payable", id);
        BigDecimal totalPaid = (p.getInitialDeposit() != null ? p.getInitialDeposit() : BigDecimal.ZERO).add(instSum);
        BigDecimal balance   = p.getTotalAmount() != null ? p.getTotalAmount().subtract(totalPaid) : null;
        model.addAttribute("instalments", instalments);
        model.addAttribute("totalPaid", totalPaid);
        model.addAttribute("balance", balance);
        return "payable/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @ModelAttribute("item") @Valid PayableTransaction p,
                         BindingResult result, Model model, RedirectAttributes attrs) {
        if (result.hasErrors()) {
            addFormData(model);
            model.addAttribute("instalments", instalmentRepo.findBySourceTypeAndSourceIdOrderByPaymentDateAsc("Payable", id));
            model.addAttribute("totalPaid", BigDecimal.ZERO);
            return "payable/form";
        }
        p.setId(id);
        BigDecimal instSum = instalmentRepo.sumBySourceTypeAndSourceId("Payable", id);
        computeStatus(p, instSum);
        repo.save(p);
        attrs.addFlashAttribute("success", "Payable transaction updated.");
        return "redirect:/payable";
    }

    @PostMapping("/{id}/pay")
    public String addPayment(@PathVariable Long id,
                              @RequestParam BigDecimal amount,
                              @RequestParam String paymentDate,
                              @RequestParam String paymentMode,
                              @RequestParam(required = false) String notes,
                              RedirectAttributes attrs) {
        PayableTransaction p = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Not found: " + id));
        PaymentInstalment inst = new PaymentInstalment();
        inst.setSourceType("Payable");
        inst.setSourceId(id);
        inst.setAmount(amount);
        inst.setPaymentDate(LocalDate.parse(paymentDate));
        inst.setPaymentMode(paymentMode);
        inst.setNotes(notes);
        instalmentRepo.save(inst);
        BigDecimal instSum = instalmentRepo.sumBySourceTypeAndSourceId("Payable", id);
        computeStatus(p, instSum);
        repo.save(p);
        attrs.addFlashAttribute("success", "Payment of £" + amount + " recorded.");
        return "redirect:/payable/" + id + "/edit";
    }

    @PostMapping("/instalments/{instId}/delete")
    public String deleteInstalment(@PathVariable Long instId, RedirectAttributes attrs) {
        PaymentInstalment inst = instalmentRepo.findById(instId)
                .orElseThrow(() -> new IllegalArgumentException("Not found: " + instId));
        Long payableId = inst.getSourceId();
        instalmentRepo.deleteById(instId);
        PayableTransaction p = repo.findById(payableId).orElse(null);
        if (p != null) {
            BigDecimal instSum = instalmentRepo.sumBySourceTypeAndSourceId("Payable", payableId);
            computeStatus(p, instSum);
            repo.save(p);
        }
        attrs.addFlashAttribute("success", "Payment removed.");
        return "redirect:/payable/" + payableId + "/edit";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes attrs) {
        instalmentRepo.findBySourceTypeAndSourceIdOrderByPaymentDateAsc("Payable", id)
                .forEach(i -> instalmentRepo.deleteById(i.getId()));
        repo.deleteById(id);
        attrs.addFlashAttribute("success", "Transaction deleted.");
        return "redirect:/payable";
    }

    // AJAX: return source record details for auto-populate
    @GetMapping("/source-data")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> sourceData(
            @RequestParam String type,
            @RequestParam Long sourceId) {
        Map<String, Object> data = new HashMap<>();
        if ("HALL_BOOKING".equals(type)) {
            hallRepo.findById(sourceId).ifPresent(h -> {
                data.put("name",            h.getHallName() != null ? h.getHallName() : "");
                data.put("organisationName", h.getBookedBy() != null ? h.getBookedBy() : "");
                data.put("payablePerson",   h.getPayableName() != null ? h.getPayableName() : "");
                data.put("sortCode",        h.getSortCode() != null ? h.getSortCode() : "");
                data.put("accountNumber",   h.getAccountNumber() != null ? h.getAccountNumber() : "");
                data.put("totalAmount",     h.getHallCharges() != null ? h.getHallCharges().toPlainString() : "");
                data.put("initialDeposit",  h.getInitialDeposit() != null ? h.getInitialDeposit().toPlainString() : "");
                data.put("paymentDate",     h.getHireDate() != null ? h.getHireDate().toString() : "");
            });
        } else if ("CATERER_BOOKING".equals(type)) {
            foodRepo.findById(sourceId).ifPresent(f -> {
                data.put("name",            f.getVendorName() != null ? f.getVendorName() : "");
                data.put("organisationName", f.getVendorName() != null ? f.getVendorName() : "");
                data.put("payablePerson",   f.getVendorPayableName() != null ? f.getVendorPayableName() : "");
                data.put("sortCode",        f.getSortCode() != null ? f.getSortCode() : "");
                data.put("accountNumber",   f.getAccountNumber() != null ? f.getAccountNumber() : "");
                data.put("totalAmount",     f.getFullAmount() != null ? f.getFullAmount().toPlainString() : "");
                data.put("initialDeposit",  f.getDepositAmount() != null ? f.getDepositAmount().toPlainString() : "");
            });
        }
        return ResponseEntity.ok(data);
    }

    private void addFormData(Model model) {
        model.addAttribute("halls", hallRepo.findAllWithEvent());
        model.addAttribute("foods", foodRepo.findAllOrdered());
    }

    private void computeStatus(PayableTransaction p, BigDecimal instSum) {
        if (p.getTotalAmount() == null) return;
        BigDecimal deposit   = p.getInitialDeposit() != null ? p.getInitialDeposit() : BigDecimal.ZERO;
        BigDecimal totalPaid = deposit.add(instSum != null ? instSum : BigDecimal.ZERO);
        if (totalPaid.compareTo(p.getTotalAmount()) >= 0)    p.setStatus("COMPLETED");
        else if (totalPaid.compareTo(BigDecimal.ZERO) > 0)   p.setStatus("PARTIAL");
        else                                                   p.setStatus("PENDING");
    }
}
