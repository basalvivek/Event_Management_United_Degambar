package com.udjcs.activity;

import com.udjcs.activity.category.ActivityCategoryService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/activities")
public class ActivityController {

    private final ActivityService service;
    private final ActivityCategoryService categoryService;

    public ActivityController(ActivityService service, ActivityCategoryService categoryService) {
        this.service = service;
        this.categoryService = categoryService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("items", service.findAll());
        return "activity/list";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("item", new Activity());
        model.addAttribute("categories", categoryService.findAll());
        return "activity/form";
    }

    @PostMapping
    public String create(@ModelAttribute("item") @Valid Activity activity,
                         BindingResult result,
                         Model model,
                         RedirectAttributes attrs) {
        if (activity.getCategoryId() == null) {
            result.rejectValue("categoryId", "NotNull", "Please select a category");
        }
        if (result.hasErrors()) {
            model.addAttribute("categories", categoryService.findAll());
            return "activity/form";
        }
        service.save(activity);
        attrs.addFlashAttribute("success", "Activity saved successfully.");
        return "redirect:/activities";
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model) {
        Activity item = service.findById(id);
        item.setCategoryId(item.getActivityCategory().getId());
        model.addAttribute("item", item);
        model.addAttribute("categories", categoryService.findAll());
        return "activity/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @ModelAttribute("item") @Valid Activity activity,
                         BindingResult result,
                         Model model,
                         RedirectAttributes attrs) {
        if (activity.getCategoryId() == null) {
            result.rejectValue("categoryId", "NotNull", "Please select a category");
        }
        if (result.hasErrors()) {
            model.addAttribute("categories", categoryService.findAll());
            return "activity/form";
        }
        activity.setId(id);
        service.save(activity);
        attrs.addFlashAttribute("success", "Activity updated successfully.");
        return "redirect:/activities";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes attrs) {
        service.deleteById(id);
        attrs.addFlashAttribute("success", "Activity deleted successfully.");
        return "redirect:/activities";
    }
}
