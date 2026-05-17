package com.udjcs.food;

import com.udjcs.finance.PaymentInstalment;
import com.udjcs.finance.PaymentInstalmentRepository;
import com.udjcs.payable.PayableTransaction;
import com.udjcs.payable.PayableTransactionRepository;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/food-registrations")
public class FoodRegistrationController {

    private final FoodRegistrationRepository repo;
    private final FoodItemRepository foodItemRepo;
    private final PaymentInstalmentRepository instalmentRepo;
    private final PayableTransactionRepository payableRepo;

    public FoodRegistrationController(FoodRegistrationRepository repo,
                                       FoodItemRepository foodItemRepo,
                                       PaymentInstalmentRepository instalmentRepo,
                                       PayableTransactionRepository payableRepo) {
        this.repo          = repo;
        this.foodItemRepo  = foodItemRepo;
        this.instalmentRepo = instalmentRepo;
        this.payableRepo   = payableRepo;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("items", repo.findAllOrdered());
        return "food/list";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("item", new FoodRegistration());
        return "food/form";
    }

    @PostMapping
    public String create(@ModelAttribute("item") @Valid FoodRegistration f,
                         BindingResult result,
                         @RequestParam(value = "foodCategory", required = false) List<String> categories,
                         @RequestParam(value = "foodName",     required = false) List<String> names,
                         @RequestParam(value = "foodQty",      required = false) List<String> qtys,
                         @RequestParam(value = "foodRating",   required = false) List<String> ratings,
                         @RequestParam(value = "member1Name",  required = false) String m1n,
                         @RequestParam(value = "member2Name",  required = false) String m2n,
                         @RequestParam(value = "member3Name",  required = false) String m3n,
                         @RequestParam(value = "member4Name",  required = false) String m4n,
                         @RequestParam(value = "member1Rating", required = false) Integer m1r,
                         @RequestParam(value = "member2Rating", required = false) Integer m2r,
                         @RequestParam(value = "member3Rating", required = false) Integer m3r,
                         @RequestParam(value = "member4Rating", required = false) Integer m4r,
                         Model model, RedirectAttributes attrs) {
        if (result.hasErrors()) return "food/form";
        applyMemberPanel(f, m1n, m2n, m3n, m4n, m1r, m2r, m3r, m4r);
        computeDerived(f);
        f.getFoodItems().clear();
        FoodRegistration saved = repo.save(f);
        saveFoodItemsDirect(saved, categories, names, qtys, ratings);
        attrs.addFlashAttribute("success", "Food registration saved.");
        return "redirect:/food-registrations";
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model) {
        FoodRegistration f = repo.findByIdWithItems(id)
                .orElseThrow(() -> new IllegalArgumentException("Not found: " + id));
        model.addAttribute("item", f);
        List<PaymentInstalment> instalments = instalmentRepo
                .findBySourceTypeAndSourceIdOrderByPaymentDateAsc("Food", id);
        BigDecimal instSum   = instalmentRepo.sumBySourceTypeAndSourceId("Food", id);
        BigDecimal totalPaid = (f.getDepositAmount() != null ? f.getDepositAmount() : BigDecimal.ZERO).add(instSum);
        BigDecimal balance   = f.getFullAmount() != null ? f.getFullAmount().subtract(totalPaid) : null;
        model.addAttribute("instalments", instalments);
        model.addAttribute("totalPaid", totalPaid);
        model.addAttribute("balance", balance);
        return "food/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @ModelAttribute("item") @Valid FoodRegistration f,
                         BindingResult result,
                         @RequestParam(value = "foodCategory", required = false) List<String> categories,
                         @RequestParam(value = "foodName",     required = false) List<String> names,
                         @RequestParam(value = "foodQty",      required = false) List<String> qtys,
                         @RequestParam(value = "foodRating",   required = false) List<String> ratings,
                         @RequestParam(value = "member1Name",  required = false) String m1n,
                         @RequestParam(value = "member2Name",  required = false) String m2n,
                         @RequestParam(value = "member3Name",  required = false) String m3n,
                         @RequestParam(value = "member4Name",  required = false) String m4n,
                         @RequestParam(value = "member1Rating", required = false) Integer m1r,
                         @RequestParam(value = "member2Rating", required = false) Integer m2r,
                         @RequestParam(value = "member3Rating", required = false) Integer m3r,
                         @RequestParam(value = "member4Rating", required = false) Integer m4r,
                         Model model, RedirectAttributes attrs) {
        if (result.hasErrors()) {
            model.addAttribute("instalments", instalmentRepo.findBySourceTypeAndSourceIdOrderByPaymentDateAsc("Food", id));
            model.addAttribute("totalPaid", BigDecimal.ZERO);
            return "food/form";
        }
        f.setId(id);
        applyMemberPanel(f, m1n, m2n, m3n, m4n, m1r, m2r, m3r, m4r);
        BigDecimal instSum = instalmentRepo.sumBySourceTypeAndSourceId("Food", id);
        computeDerived(f);
        computePaymentStatus(f, instSum);
        f.getFoodItems().clear();
        FoodRegistration saved = repo.save(f);
        foodItemRepo.deleteByFoodRegistrationId(id);
        saveFoodItemsDirect(saved, categories, names, qtys, ratings);
        attrs.addFlashAttribute("success", "Food registration updated.");
        return "redirect:/food-registrations";
    }

    @PostMapping("/{id}/pay")
    public String addPayment(@PathVariable Long id,
                              @RequestParam BigDecimal amount,
                              @RequestParam String paymentDate,
                              @RequestParam String paymentMode,
                              @RequestParam(required = false) String notes,
                              RedirectAttributes attrs) {
        FoodRegistration f = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Not found: " + id));
        PaymentInstalment inst = new PaymentInstalment();
        inst.setSourceType("Food");
        inst.setSourceId(id);
        inst.setAmount(amount);
        inst.setPaymentDate(LocalDate.parse(paymentDate));
        inst.setPaymentMode(paymentMode);
        inst.setNotes(notes);
        instalmentRepo.save(inst);
        BigDecimal instSum = instalmentRepo.sumBySourceTypeAndSourceId("Food", id);
        computePaymentStatus(f, instSum);
        repo.save(f);
        createPayableEntry(f, inst);
        attrs.addFlashAttribute("success", "Payment of £" + amount + " recorded.");
        return "redirect:/food-registrations/" + id + "/edit";
    }

    @PostMapping("/instalments/{instId}/delete")
    public String deleteInstalment(@PathVariable Long instId, RedirectAttributes attrs) {
        PaymentInstalment inst = instalmentRepo.findById(instId)
                .orElseThrow(() -> new IllegalArgumentException("Not found: " + instId));
        Long foodId = inst.getSourceId();
        instalmentRepo.deleteById(instId);
        FoodRegistration f = repo.findById(foodId).orElse(null);
        // Delete the linked journal entry in Payable
        payableRepo.findBySourceTypeAndSourceId("FOOD_INST", instId)
                .ifPresent(pt -> payableRepo.delete(pt));
        if (f != null) {
            BigDecimal instSum = instalmentRepo.sumBySourceTypeAndSourceId("Food", foodId);
            computePaymentStatus(f, instSum);
            repo.save(f);
        }
        attrs.addFlashAttribute("success", "Payment removed.");
        return "redirect:/food-registrations/" + foodId + "/edit";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes attrs) {
        foodItemRepo.deleteByFoodRegistrationId(id);
        instalmentRepo.findBySourceTypeAndSourceIdOrderByPaymentDateAsc("Food", id)
                .forEach(i -> instalmentRepo.deleteById(i.getId()));
        repo.deleteById(id);
        attrs.addFlashAttribute("success", "Food registration deleted.");
        return "redirect:/food-registrations";
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void saveFoodItemsDirect(FoodRegistration parent, List<String> categories,
                                      List<String> names, List<String> qtys, List<String> ratings) {
        if (categories == null || categories.isEmpty()) return;
        List<FoodItem> toSave = new ArrayList<>();
        for (int i = 0; i < categories.size(); i++) {
            String cat  = categories.get(i);
            String name = names  != null && i < names.size()   ? names.get(i)   : "";
            if ((cat == null || cat.isBlank()) && (name == null || name.isBlank())) continue;
            FoodItem item = new FoodItem();
            item.setFoodRegistration(parent);
            item.setFoodCategory(cat);
            item.setFoodName(name);
            if (qtys != null && i < qtys.size() && !qtys.get(i).isBlank()) {
                try { item.setQuantity(Integer.parseInt(qtys.get(i))); } catch (NumberFormatException ignored) {}
            }
            if (ratings != null && i < ratings.size() && !ratings.get(i).isBlank()) {
                try { item.setRating(Integer.parseInt(ratings.get(i))); } catch (NumberFormatException ignored) {}
            }
            toSave.add(item);
        }
        foodItemRepo.saveAll(toSave);
    }

    private void applyMemberPanel(FoodRegistration f,
                                   String m1n, String m2n, String m3n, String m4n,
                                   Integer m1r, Integer m2r, Integer m3r, Integer m4r) {
        f.setMember1Name(m1n); f.setMember1Rating(m1r);
        f.setMember2Name(m2n); f.setMember2Rating(m2r);
        f.setMember3Name(m3n); f.setMember3Rating(m3r);
        f.setMember4Name(m4n); f.setMember4Rating(m4r);
    }

    private void computeDerived(FoodRegistration f) {
        List<Integer> rs = new ArrayList<>();
        if (f.getMember1Rating() != null) rs.add(f.getMember1Rating());
        if (f.getMember2Rating() != null) rs.add(f.getMember2Rating());
        if (f.getMember3Rating() != null) rs.add(f.getMember3Rating());
        if (f.getMember4Rating() != null) rs.add(f.getMember4Rating());
        if (!rs.isEmpty()) {
            double avg = rs.stream().mapToInt(Integer::intValue).average().orElse(0);
            f.setOverallFoodRating(BigDecimal.valueOf(avg).setScale(1, RoundingMode.HALF_UP));
        }
        long lowCount = rs.stream().filter(r -> r <= 2).count();
        if (lowCount >= 3 && !"ACCEPTED".equals(f.getFoodSelectionStatus())) {
            f.setFoodSelectionStatus("REJECTED");
        }
    }

    private void computePaymentStatus(FoodRegistration f, BigDecimal instSum) {
        if (f.getFullAmount() == null) return;
        BigDecimal deposit   = f.getDepositAmount() != null ? f.getDepositAmount() : BigDecimal.ZERO;
        BigDecimal totalPaid = deposit.add(instSum != null ? instSum : BigDecimal.ZERO);
        f.setPaymentStatus(totalPaid.compareTo(f.getFullAmount()) >= 0 ? "Completed" : "InProgress");
    }

    private void createPayableEntry(FoodRegistration f, PaymentInstalment inst) {
        PayableTransaction pt = new PayableTransaction();
        pt.setPaymentType("CATERER_BOOKING");
        pt.setSourceType("FOOD_INST");
        pt.setSourceId(inst.getId());
        pt.setName(f.getVendorName() != null ? f.getVendorName() : "Catering / Food");
        pt.setOrganisationName(f.getVendorName());
        pt.setPayablePerson(f.getVendorPayableName());
        pt.setSortCode(f.getSortCode());
        pt.setAccountNumber(f.getAccountNumber());
        pt.setTotalAmount(inst.getAmount());
        pt.setInitialDeposit(inst.getAmount());
        pt.setPaymentDate(inst.getPaymentDate());
        pt.setStatus("COMPLETED");
        pt.setNotes(inst.getPaymentMode() + (inst.getNotes() != null ? " — " + inst.getNotes() : ""));
        payableRepo.save(pt);
    }
}
