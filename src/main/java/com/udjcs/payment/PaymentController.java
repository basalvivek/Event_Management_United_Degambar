package com.udjcs.payment;

import com.udjcs.supportive.SupportiveOrganizationService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentService service;
    private final SupportiveOrganizationService organizationService;

    public PaymentController(PaymentService service, SupportiveOrganizationService organizationService) {
        this.service = service;
        this.organizationService = organizationService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("items", service.findAll());
        return "payment/list";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("item", new Payment());
        model.addAttribute("organizations", organizationService.findAll());
        return "payment/form";
    }

    @PostMapping
    public String create(@ModelAttribute("item") @Valid Payment payment,
                         BindingResult result,
                         Model model,
                         RedirectAttributes attrs) {
        if (result.hasErrors()) {
            model.addAttribute("organizations", organizationService.findAll());
            return "payment/form";
        }
        service.save(payment);
        attrs.addFlashAttribute("success", "Payment saved successfully.");
        return "redirect:/payments";
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model) {
        Payment item = service.findById(id);
        if (item.getSupportiveOrganization() != null) {
            item.setOrganizationId(item.getSupportiveOrganization().getId());
        }
        model.addAttribute("item", item);
        model.addAttribute("organizations", organizationService.findAll());
        return "payment/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @ModelAttribute("item") @Valid Payment payment,
                         BindingResult result,
                         Model model,
                         RedirectAttributes attrs) {
        if (result.hasErrors()) {
            model.addAttribute("organizations", organizationService.findAll());
            return "payment/form";
        }
        payment.setId(id);
        service.save(payment);
        attrs.addFlashAttribute("success", "Payment updated successfully.");
        return "redirect:/payments";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes attrs) {
        service.deleteById(id);
        attrs.addFlashAttribute("success", "Payment deleted successfully.");
        return "redirect:/payments";
    }
}
