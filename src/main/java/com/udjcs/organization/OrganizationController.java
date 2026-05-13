package com.udjcs.organization;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.List;

@Controller
@RequestMapping("/organization")
public class OrganizationController {

    private final OrganizationService service;

    public OrganizationController(OrganizationService service) {
        this.service = service;
    }

    @GetMapping
    public String list() {
        List<Organization> all = service.findAll();
        if (all.isEmpty()) {
            return "redirect:/organization/new";
        }
        return "redirect:/organization/" + all.get(0).getId() + "/edit";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("item", new Organization());
        return "organization/form";
    }

    @PostMapping
    public String create(@ModelAttribute("item") @Valid Organization organization,
                         BindingResult result,
                         @RequestParam("logoFile") MultipartFile logoFile,
                         RedirectAttributes attrs) throws IOException {
        if (result.hasErrors()) return "organization/form";
        service.save(organization, logoFile);
        attrs.addFlashAttribute("success", "Organization saved successfully.");
        return "redirect:/organization";
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model) {
        model.addAttribute("item", service.findById(id));
        return "organization/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @ModelAttribute("item") @Valid Organization organization,
                         BindingResult result,
                         @RequestParam("logoFile") MultipartFile logoFile,
                         RedirectAttributes attrs) throws IOException {
        if (result.hasErrors()) return "organization/form";
        organization.setId(id);
        service.save(organization, logoFile);
        attrs.addFlashAttribute("success", "Organization updated successfully.");
        return "redirect:/organization";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes attrs) {
        service.deleteById(id);
        attrs.addFlashAttribute("success", "Organization deleted.");
        return "redirect:/organization";
    }
}
