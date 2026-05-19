package com.udjcs.hall;

import com.udjcs.event.EventRepository;
import com.udjcs.finance.PaymentInstalment;
import com.udjcs.finance.PaymentInstalmentRepository;
import com.udjcs.payable.PayableTransaction;
import com.udjcs.payable.PayableTransactionRepository;
import com.udjcs.venue.VenueRepository;
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
    private final VenueRepository venueRepo;

    public HallRegistrationController(HallRegistrationRepository repo,
                                       EventRepository eventRepo,
                                       PaymentInstalmentRepository instalmentRepo,
                                       PayableTransactionRepository payableRepo,
                                       VenueRepository venueRepo) {
        this.repo          = repo;
        this.eventRepo     = eventRepo;
        this.instalmentRepo = instalmentRepo;
        this.payableRepo   = payableRepo;
        this.venueRepo     = venueRepo;
    }

    @GetMapping
    public String list(Model model) {
        List<HallRegistration> bookings = repo.findAllWithEvent();

        // Group bookings by event name
        java.util.LinkedHashMap<String, List<HallRegistration>> groups = new java.util.LinkedHashMap<>();
        bookings.stream()
            .sorted(java.util.Comparator.comparing(h ->
                h.getEvent() != null ? h.getEvent().getEventName().toLowerCase() : "zzz"))
            .forEach(h -> {
                String evKey = h.getEvent() != null ? h.getEvent().getEventName() : "— No Event —";
                groups.computeIfAbsent(evKey, k -> new java.util.ArrayList<>()).add(h);
            });

        // Fetch instalments and totals for each booking
        java.util.Map<Long, List<PaymentInstalment>> instalmentMap = new java.util.LinkedHashMap<>();
        java.util.Map<Long, BigDecimal> totalPaidMap = new java.util.LinkedHashMap<>();
        java.util.Map<Long, BigDecimal> remainingMap = new java.util.LinkedHashMap<>();

        for (HallRegistration h : bookings) {
            List<PaymentInstalment> insts = instalmentRepo
                    .findBySourceTypeAndSourceIdOrderByPaymentDateAsc("Hall", h.getId());
            instalmentMap.put(h.getId(), insts);
            BigDecimal instSum = insts.stream().map(PaymentInstalment::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal deposit = h.getInitialDeposit() != null ? h.getInitialDeposit() : BigDecimal.ZERO;
            BigDecimal paid = deposit.add(instSum);
            BigDecimal charges = h.getHallCharges() != null ? h.getHallCharges() : BigDecimal.ZERO;
            totalPaidMap.put(h.getId(), paid);
            remainingMap.put(h.getId(), charges.subtract(paid).max(BigDecimal.ZERO));
        }

        model.addAttribute("groups", groups);
        model.addAttribute("instalmentMap", instalmentMap);
        model.addAttribute("totalPaidMap", totalPaidMap);
        model.addAttribute("remainingMap", remainingMap);
        return "hall/list";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("item", new HallRegistration());
        model.addAttribute("events", eventRepo.findAll(Sort.by("eventName")));
        model.addAttribute("venues", venueRepo.findAll(Sort.by("venueName")));
        return "hall/form";
    }

    @PostMapping
    public String create(@ModelAttribute("item") @Valid HallRegistration h,
                         BindingResult result, Model model, RedirectAttributes attrs) {
        if (h.getEventId() == null) {
            result.rejectValue("eventId", "required", "Please select an event.");
        }
        if (result.hasErrors()) {
            model.addAttribute("events", eventRepo.findAll(Sort.by("eventName")));
            model.addAttribute("venues", venueRepo.findAll(Sort.by("venueName")));
            return "hall/form";
        }
        wireEvent(h);
        h.setStatus(computeStatus(h, BigDecimal.ZERO));
        repo.save(h);
        if (h.getInitialDeposit() != null && h.getInitialDeposit().compareTo(BigDecimal.ZERO) > 0) {
            createDepositPayableEntry(h);
        }
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
        model.addAttribute("venues", venueRepo.findAll(Sort.by("venueName")));
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
            model.addAttribute("venues", venueRepo.findAll(Sort.by("venueName")));
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
                .forEach(i -> {
                    payableRepo.findBySourceTypeAndSourceId("HALL_INST", i.getId())
                            .ifPresent(pt -> payableRepo.delete(pt));
                    instalmentRepo.deleteById(i.getId());
                });
        payableRepo.findBySourceTypeAndSourceId("HALL_DEPOSIT", id)
                .ifPresent(pt -> payableRepo.delete(pt));
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

    private void createDepositPayableEntry(HallRegistration h) {
        payableRepo.findBySourceTypeAndSourceId("HALL_DEPOSIT", h.getId())
                .ifPresent(pt -> payableRepo.delete(pt));
        PayableTransaction pt = new PayableTransaction();
        pt.setPaymentType("HALL_BOOKING");
        pt.setSourceType("HALL_DEPOSIT");
        pt.setSourceId(h.getId());
        pt.setName(h.getHallName() != null ? h.getHallName() : "Hall Booking");
        pt.setOrganisationName(h.getBookedBy());
        pt.setPayablePerson(h.getPayableName());
        pt.setSortCode(h.getSortCode());
        pt.setAccountNumber(h.getAccountNumber());
        pt.setTotalAmount(h.getHallCharges());
        pt.setInitialDeposit(h.getInitialDeposit());
        pt.setPaymentDate(h.getHireDate());
        pt.setStatus("COMPLETED");
        pt.setNotes("Initial Deposit");
        pt.setEvent(h.getEvent());
        payableRepo.save(pt);
    }

    private void createPayableEntry(HallRegistration h, PaymentInstalment inst) {
        PayableTransaction pt = new PayableTransaction();
        pt.setPaymentType("HALL_BOOKING");
        pt.setSourceType("HALL_INST");
        pt.setSourceId(inst.getId());
        pt.setName(h.getHallName() != null ? h.getHallName() : "Hall Booking");
        pt.setOrganisationName(h.getBookedBy());
        pt.setPayablePerson(h.getPayableName());
        pt.setSortCode(h.getSortCode());
        pt.setAccountNumber(h.getAccountNumber());
        pt.setTotalAmount(inst.getAmount());
        pt.setInitialDeposit(inst.getAmount());
        pt.setPaymentDate(inst.getPaymentDate());
        pt.setStatus("COMPLETED");
        pt.setNotes(inst.getPaymentMode() + (inst.getNotes() != null ? " — " + inst.getNotes() : ""));
        pt.setEvent(h.getEvent());
        payableRepo.save(pt);
    }
}
