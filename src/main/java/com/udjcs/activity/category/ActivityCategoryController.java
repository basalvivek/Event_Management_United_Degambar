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
    public String delete(@PathVariable Long id, RedirectAttributes attrs) {
        service.deleteById(id);
        attrs.addFlashAttribute("success", "Activity category deleted successfully.");
        return "redirect:/activity-categories";
    }
}
