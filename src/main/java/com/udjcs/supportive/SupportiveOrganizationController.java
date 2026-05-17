package com.udjcs.supportive;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

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

    @PostMapping("/quick-add")
    @ResponseBody
    public ResponseEntity<Map<String, String>> quickAdd(
            @RequestParam String name,
            @RequestParam(required = false) String organizationType,
            @RequestParam(required = false) String contactPerson,
            @RequestParam(required = false) String contactPhone) throws IOException {
        SupportiveOrganization org = new SupportiveOrganization();
        org.setName(name.trim());
        org.setOrganizationType(organizationType != null && !organizationType.isBlank() ? organizationType.trim() : null);
        org.setContactPerson(contactPerson != null && !contactPerson.isBlank() ? contactPerson.trim() : null);
        org.setContactPhone(contactPhone != null && !contactPhone.isBlank() ? contactPhone.trim() : null);
        service.save(org, null);
        Map<String, String> result = new LinkedHashMap<>();
        result.put("name", org.getName());
        return ResponseEntity.ok(result);
    }
}
