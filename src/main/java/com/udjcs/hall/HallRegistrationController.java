package com.udjcs.hall;

import com.udjcs.event.EventRepository;
import com.udjcs.finance.PaymentInstalment;
import com.udjcs.finance.PaymentInstalmentRepository;
import com.udjcs.payable.PayableTransaction;
import com.udjcs.payable.PayableTransactionRepository;
import jakarta.validation.Valid;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.ArrayList;

@Controller
@RequestMapping("/hall-registrations")
public class HallRegistrationController {

    private final HallRegistrationRepository repo;
    private final EventRepository eventRepo;
    private final PaymentInstalmentRepository instalmentRepo;
    private final PayableTransactionRepository payableRepo;

    public HallRegistrationController(HallRegistrationRepository repo,
                                       EventRepository eventRepo,
                                       PaymentInstalmentRepository instalmentRepo,
                                       PayableTransactionRepository payableRepo) {
        this.repo          = repo;
        this.eventRepo     = eventRepo;
        this.instalmentRepo = instalmentRepo;
        this.payableRepo   = payableRepo;
    }

    @GetMapping
    public String list(Model model) {
        List<HallRegistration> bookings = repo.findAllWithEvent();
        List<HallPaymentRow> rows = new java.util.ArrayList<>();

        for (HallRegistration h : bookings) {
            List<PaymentInstalment> insts = instalmentRepo
                    .findBySourceTypeAndSourceIdOrderByPaymentDateAsc("Hall", h.getId());
            String eventName = h.getEvent() != null ? h.getEvent().getEventName() : "— No Event —";
            BigDecimal charges = h.getHallCharges() != null ? h.getHallCharges() : BigDecimal.ZERO;

            if (insts.isEmpty()) {
                // No instalments — show booking as single row
                HallPaymentRow row = new HallPaymentRow();
                row.setBookingId(h.getId());
                row.setEventName(eventName);
                row.setHallName(h.getHallName());
                row.setBookedBy(h.getBookedBy());
                row.setPhone(h.getPhone());
                row.setHireDate(h.getHireDate());
                row.setFromTime(h.getFromTime());
                row.setTillTime(h.getTillTime());
                row.setHallCharges(charges);
                row.setBookingStatus(h.getBookingStatus());
                row.setPaymentStatus(h.getStatus());
                row.setPaymentDate(h.getHireDate());
                row.setPaymentMode("—");
                BigDecimal deposit = h.getInitialDeposit() != null ? h.getInitialDeposit() : BigDecimal.ZERO;
                row.setAmountPaid(deposit);
                row.setRemaining(charges.subtract(deposit).max(BigDecimal.ZERO));
                row.setNotes(h.getNotes());
                rows.add(row);
            } else {
                BigDecimal running = BigDecimal.ZERO;

                // Initial deposit row (first, before instalments)
                BigDecimal deposit = h.getInitialDeposit() != null ? h.getInitialDeposit() : BigDecimal.ZERO;
                if (deposit.compareTo(BigDecimal.ZERO) > 0) {
                    running = running.add(deposit);
                    HallPaymentRow depRow = new HallPaymentRow();
                    depRow.setBookingId(h.getId());
                    depRow.setEventName(eventName);
                    depRow.setHallName(h.getHallName());
                    depRow.setBookedBy(h.getBookedBy());
                    depRow.setPhone(h.getPhone());
                    depRow.setHireDate(h.getHireDate());
                    depRow.setFromTime(h.getFromTime());
                    depRow.setTillTime(h.getTillTime());
                    depRow.setHallCharges(charges);
                    depRow.setBookingStatus(h.getBookingStatus());
                    depRow.setPaymentStatus(h.getStatus());
                    depRow.setPaymentDate(h.getHireDate());
                    depRow.setPaymentMode("Initial Deposit");
                    depRow.setAmountPaid(deposit);
                    depRow.setRemaining(charges.subtract(running).max(BigDecimal.ZERO));
                    depRow.setNotes("Paid at booking");
                    rows.add(depRow);
                }

                for (PaymentInstalment inst : insts) {
                    running = running.add(inst.getAmount());
                    HallPaymentRow row = new HallPaymentRow();
                    row.setBookingId(h.getId());
                    row.setEventName(eventName);
                    row.setHallName(h.getHallName());
                    row.setBookedBy(h.getBookedBy());
                    row.setPhone(h.getPhone());
                    row.setHireDate(h.getHireDate());
                    row.setFromTime(h.getFromTime());
                    row.setTillTime(h.getTillTime());
                    row.setHallCharges(charges);
                    row.setBookingStatus(h.getBookingStatus());
                    row.setPaymentStatus(h.getStatus());
                    row.setPaymentDate(inst.getPaymentDate());
                    row.setPaymentMode(inst.getPaymentMode());
                    row.setAmountPaid(inst.getAmount());
                    row.setRemaining(charges.subtract(running).max(BigDecimal.ZERO));
                    row.setNotes(inst.getNotes());
                    rows.add(row);
                }
            }
        }

        // Sort: event name ASC, then payment date DESC within group
        rows.sort((a, b) -> {
            int cmp = a.getEventName().compareToIgnoreCase(b.getEventName());
            if (cmp != 0) return cmp;
            LocalDate da = a.getPaymentDate() != null ? a.getPaymentDate() : java.time.LocalDate.MIN;
            LocalDate db = b.getPaymentDate() != null ? b.getPaymentDate() : java.time.LocalDate.MIN;
            return db.compareTo(da);
        });

        // Compute group-start indices by event name
        java.util.Set<Integer> groupStarts = new java.util.HashSet<>();
        String prev = null;
        for (int i = 0; i < rows.size(); i++) {
            String key = rows.get(i).getEventName().toLowerCase();
            if (!key.equals(prev)) { groupStarts.add(i); prev = key; }
        }

        model.addAttribute("rows", rows);
        model.addAttribute("groupStarts", groupStarts);
        return "hall/list";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("item", new HallRegistration());
        model.addAttribute("events", eventRepo.findAll(Sort.by("eventName")));
        return "hall/form";
    }

    @PostMapping
    public String create(@ModelAttribute("item") @Valid HallRegistration h,
                         BindingResult result, Model model, RedirectAttributes attrs) {
        if (result.hasErrors()) {
            model.addAttribute("events", eventRepo.findAll(Sort.by("eventName")));
            return "hall/form";
        }
        wireEvent(h);
        h.setStatus(computeStatus(h, BigDecimal.ZERO));
        repo.save(h);
        attrs.addFlashAttribute("success", "Hall registration saved.");
        return "redirect:/hall-registrations";
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model) {
        HallRegistration h = repo.findByIdWithEvent(id)
                .orElseThrow(() -> new IllegalArgumentException("Not found: " + id));
        if (h.getEvent() != null) h.setEventId(h.getEvent().getId());
        model.addAttribute("item", h);
        model.addAttribute("events", eventRepo.findAll(Sort.by("eventName")));
        List<PaymentInstalment> instalments = instalmentRepo
                .findBySourceTypeAndSourceIdOrderByPaymentDateAsc("Hall", id);
        BigDecimal instalmentSum = instalmentRepo.sumBySourceTypeAndSourceId("Hall", id);
        model.addAttribute("instalments", instalments);
        model.addAttribute("instalmentSum", instalmentSum);
        BigDecimal totalPaid = (h.getInitialDeposit() != null ? h.getInitialDeposit() : BigDecimal.ZERO)
                .add(instalmentSum);
        model.addAttribute("totalPaid", totalPaid);
        BigDecimal remaining = h.getHallCharges() != null ? h.getHallCharges().subtract(totalPaid) : null;
        model.addAttribute("remaining", remaining);
        return "hall/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @ModelAttribute("item") @Valid HallRegistration h,
                         BindingResult result, Model model, RedirectAttributes attrs) {
        if (result.hasErrors()) {
            model.addAttribute("events", eventRepo.findAll(Sort.by("eventName")));
            return "hall/form";
        }
        h.setId(id);
        wireEvent(h);
        BigDecimal instalmentSum = instalmentRepo.sumBySourceTypeAndSourceId("Hall", id);
        h.setStatus(computeStatus(h, instalmentSum));
        repo.save(h);
        attrs.addFlashAttribute("success", "Hall registration updated.");
        return "redirect:/hall-registrations";
    }

    @PostMapping("/{id}/pay")
    public String addPayment(@PathVariable Long id,
                              @RequestParam BigDecimal amount,
                              @RequestParam String paymentDate,
                              @RequestParam String paymentMode,
                              @RequestParam(required = false) String notes,
                              RedirectAttributes attrs) {
        HallRegistration h = repo.findByIdWithEvent(id)
                .orElseThrow(() -> new IllegalArgumentException("Not found: " + id));
        PaymentInstalment inst = new PaymentInstalment();
        inst.setSourceType("Hall");
        inst.setSourceId(id);
        inst.setAmount(amount);
        inst.setPaymentDate(LocalDate.parse(paymentDate));
        inst.setPaymentMode(paymentMode);
        inst.setNotes(notes);
        instalmentRepo.save(inst);
        BigDecimal instalmentSum = instalmentRepo.sumBySourceTypeAndSourceId("Hall", id);
        h.setStatus(computeStatus(h, instalmentSum));
        repo.save(h);
        createPayableEntry(h, inst);
        attrs.addFlashAttribute("success", "Payment of £" + amount + " recorded.");
        return "redirect:/hall-registrations/" + id + "/edit";
    }

    @PostMapping("/instalments/{instId}/delete")
    public String deleteInstalment(@PathVariable Long instId, RedirectAttributes attrs) {
        PaymentInstalment inst = instalmentRepo.findById(instId)
                .orElseThrow(() -> new IllegalArgumentException("Not found: " + instId));
        Long hallId = inst.getSourceId();
        instalmentRepo.deleteById(instId);
        // Delete the linked journal entry in Payable
        payableRepo.findBySourceTypeAndSourceId("HALL_INST", instId)
                .ifPresent(pt -> payableRepo.delete(pt));
        HallRegistration h = repo.findByIdWithEvent(hallId).orElse(null);
        if (h != null) {
            BigDecimal instalmentSum = instalmentRepo.sumBySourceTypeAndSourceId("Hall", hallId);
            h.setStatus(computeStatus(h, instalmentSum));
            repo.save(h);
        }
        attrs.addFlashAttribute("success", "Payment removed.");
        return "redirect:/hall-registrations/" + hallId + "/edit";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes attrs) {
        instalmentRepo.findBySourceTypeAndSourceIdOrderByPaymentDateAsc("Hall", id)
                .forEach(i -> instalmentRepo.deleteById(i.getId()));
        repo.deleteById(id);
        attrs.addFlashAttribute("success", "Hall registration deleted.");
        return "redirect:/hall-registrations";
    }

    private void wireEvent(HallRegistration h) {
        if (h.getEventId() != null) {
            eventRepo.findById(h.getEventId()).ifPresent(h::setEvent);
        } else {
            h.setEvent(null);
        }
    }

    private String computeStatus(HallRegistration h, BigDecimal instalmentSum) {
        if (h.getHallCharges() == null) return "InProgress";
        BigDecimal deposit = h.getInitialDeposit() != null ? h.getInitialDeposit() : BigDecimal.ZERO;
        BigDecimal total = deposit.add(instalmentSum != null ? instalmentSum : BigDecimal.ZERO);
        return total.compareTo(h.getHallCharges()) >= 0 ? "Completed" : "InProgress";
    }

    private void createPayableEntry(HallRegistration h, PaymentInstalment inst) {
        PayableTransaction pt = new PayableTransaction();
        pt.setPaymentType("HALL_BOOKING");
        pt.setSourceType("HALL_INST");
        pt.setSourceId(inst.getId());
        pt.setName((h.getHallName() != null ? h.getHallName() : "Hall Booking") +
                   (h.getEvent() != null ? " — " + h.getEvent().getEventName() : ""));
        pt.setOrganisationName(h.getBookedBy());
        pt.setPayablePerson(h.getPayableName());
        pt.setSortCode(h.getSortCode());
        pt.setAccountNumber(h.getAccountNumber());
        pt.setTotalAmount(inst.getAmount());
        pt.setInitialDeposit(inst.getAmount());
        pt.setPaymentDate(inst.getPaymentDate());
        pt.setStatus("COMPLETED");
        pt.setNotes(inst.getPaymentMode() + (inst.getNotes() != null ? " — " + inst.getNotes() : ""));
        payableRepo.save(pt);
    }
}
