package com.udjcs.supportive;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;

@Controller
@RequestMapping("/supportive")
public class SupportiveOrganizationController {

    private final SupportiveOrganizationService service;

    public SupportiveOrganizationController(SupportiveOrganizationService service) {
        this.service = service;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("items", service.findAll());
        return "supportive/list";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("item", new SupportiveOrganization());
        return "supportive/form";
    }

    @PostMapping
    public String create(@ModelAttribute("item") @Valid SupportiveOrganization org,
                         BindingResult result,
                         @RequestParam(value = "logoFile", required = false) MultipartFile logoFile,
                         RedirectAttributes attrs) throws IOException {
        if (result.hasErrors()) return "supportive/form";
        service.save(org, logoFile);
        attrs.addFlashAttribute("success", "Supportive organization saved successfully.");
        return "redirect:/supportive";
    }

    @GetMapping("/{id}")
    public String redirectToEdit(@PathVariable Long id) {
        return "redirect:/supportive/" + id + "/edit";
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model) {
        model.addAttribute("item", service.findById(id));
        return "supportive/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @ModelAttribute("item") @Valid SupportiveOrganization org,
                         BindingResult result,
                         @RequestParam(value = "logoFile", required = false) MultipartFile logoFile,
                         RedirectAttributes attrs) throws IOException {
        if (result.hasErrors()) return "supportive/form";
        org.setId(id);
        service.save(org, logoFile);
        attrs.addFlashAttribute("success", "Supportive organization updated successfully.");
        return "redirect:/supportive";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes attrs) {
        service.deleteById(id);
        attrs.addFlashAttribute("success", "Supportive organization deleted.");
        return "redirect:/supportive";
    }
}
