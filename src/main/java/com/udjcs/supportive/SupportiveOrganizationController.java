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
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/supportive")
public class SupportiveOrganizationController {

    private final SupportiveOrganizationService service;

    public SupportiveOrganizationController(SupportiveOrganizationService service) {
        this.service = service;
    }

    @GetMapping
    public String list(Model model) {
        List<SupportiveOrganization> all = service.findAll();

        LinkedHashMap<String, List<SupportiveOrganization>> grouped = all.stream()
            .sorted(Comparator.comparing(o -> o.getName() != null ? o.getName().toLowerCase() : ""))
            .collect(Collectors.groupingBy(
                o -> (o.getOrganizationType() != null && !o.getOrganizationType().isBlank())
                     ? o.getOrganizationType() : "Uncategorised",
                LinkedHashMap::new,
                Collectors.toList()
            ));

        LinkedHashMap<String, List<SupportiveOrganization>> sortedGrouped = new LinkedHashMap<>();
        grouped.entrySet().stream()
            .sorted(Map.Entry.comparingByKey(
                Comparator.comparing(k -> k.equals("Uncategorised") ? "zzz" : k.toLowerCase())
            ))
            .forEach(e -> sortedGrouped.put(e.getKey(), e.getValue()));

        model.addAttribute("groupedItems", sortedGrouped);
        model.addAttribute("items", all);
        return "supportive/list";
    }

    @GetMapping("/new")
    public String showCreateForm(@RequestParam(required = false) String type, Model model) {
        SupportiveOrganization item = new SupportiveOrganization();
        boolean isSponsor = "sponsor".equals(type);
        if (isSponsor) item.setOrganizationType("Sponsor");
        model.addAttribute("item", item);
        model.addAttribute("formType", isSponsor ? "sponsor" : "org");
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
        SupportiveOrganization item = service.findById(id);
        model.addAttribute("item", item);
        String ot = item.getOrganizationType();
        boolean isSponsor = "Sponsor".equals(ot)
                || "Group Sponsor".equals(ot)
                || "Individual Sponsor".equals(ot)
                || (item.getSponsorshipType() != null && !item.getSponsorshipType().isBlank());
        model.addAttribute("formType", isSponsor ? "sponsor" : "org");
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
