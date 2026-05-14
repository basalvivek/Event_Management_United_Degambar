package com.udjcs.activity.category;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/activity-categories")
public class ActivityCategoryController {

    private final ActivityCategoryService service;

    public ActivityCategoryController(ActivityCategoryService service) {
        this.service = service;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("items", service.findAll());
        return "activity-category/list";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("item", new ActivityCategory());
        return "activity-category/form";
    }

    @PostMapping
    public String create(@ModelAttribute("item") @Valid ActivityCategory category,
                         BindingResult result,
                         RedirectAttributes attrs) {
        if (result.hasErrors()) return "activity-category/form";
        service.save(category);
        attrs.addFlashAttribute("success", "Activity category saved successfully.");
        return "redirect:/activity-categories";
    }

    @GetMapping("/{id}")
    public String redirectToEdit(@PathVariable Long id) {
        return "redirect:/activity-categories/" + id + "/edit";
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model) {
        model.addAttribute("item", service.findById(id));
        return "activity-category/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @ModelAttribute("item") @Valid ActivityCategory category,
                         BindingResult result,
                         RedirectAttributes attrs) {
        if (result.hasErrors()) return "activity-category/form";
        category.setId(id);
        service.save(category);
        attrs.addFlashAttribute("success", "Activity category updated successfully.");
        return "redirect:/activity-categories";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id,
                         @RequestParam(required = false) String returnTo,
                         RedirectAttributes attrs) {
        service.deleteById(id);
        attrs.addFlashAttribute("success", "Category deleted.");
        return "redirect:" + (returnTo != null && !returnTo.isBlank() ? returnTo : "/activity-categories");
    }

    @PostMapping("/quick-add")
    public String quickAdd(@RequestParam(required = false) String categoryName,
                           @RequestParam(required = false) String returnTo,
                           RedirectAttributes attrs) {
        if (categoryName != null && !categoryName.isBlank()) {
            ActivityCategory cat = new ActivityCategory();
            cat.setCategoryName(categoryName.trim());
            cat.setStatus("Active");
            service.save(cat);
            attrs.addFlashAttribute("success", "Category \"" + categoryName.trim() + "\" added.");
        }
        return "redirect:" + (returnTo != null && !returnTo.isBlank() ? returnTo : "/activities/new");
    }
}
