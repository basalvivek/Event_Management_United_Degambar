package com.udjcs.progress;

import com.udjcs.activity.ActivityService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/progress")
public class ActivityProgressController {

    private final ActivityProgressService service;
    private final ActivityService activityService;

    public ActivityProgressController(ActivityProgressService service,
                                      ActivityService activityService) {
        this.service = service;
        this.activityService = activityService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("items", service.findAll());
        return "progress/list";
    }

    @GetMapping("/new")
    public String showCreateForm(@RequestParam(required = false) Long activityId, Model model) {
        ActivityProgress item = new ActivityProgress();
        item.setProgressDate(java.time.LocalDate.now());
        if (activityId != null) {
            item.setActivityId(activityId);
            model.addAttribute("preselectedActivity", activityService.findById(activityId));
        } else {
            model.addAttribute("preselectedActivity", null);
        }
        model.addAttribute("item", item);
        model.addAttribute("activities", activityService.findAll());
        return "progress/form";
    }

    @PostMapping
    public String create(@ModelAttribute("item") @Valid ActivityProgress progress,
                         BindingResult result,
                         Model model,
                         RedirectAttributes attrs) {
        if (progress.getActivityId() == null) {
            result.rejectValue("activityId", "NotNull", "Please select an activity");
        }
        if (result.hasErrors()) {
            model.addAttribute("activities", activityService.findAll());
            return "progress/form";
        }
        service.save(progress);
        attrs.addFlashAttribute("success", "Progress record saved successfully.");
        return "redirect:/progress";
    }

    @GetMapping("/{id}")
    public String redirectToEdit(@PathVariable Long id) {
        return "redirect:/progress/" + id + "/edit";
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model) {
        ActivityProgress item = service.findById(id);
        item.setActivityId(item.getActivity().getId());
        model.addAttribute("item", item);
        model.addAttribute("activities", activityService.findAll());
        return "progress/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @ModelAttribute("item") @Valid ActivityProgress progress,
                         BindingResult result,
                         Model model,
                         RedirectAttributes attrs) {
        if (progress.getActivityId() == null) {
            result.rejectValue("activityId", "NotNull", "Please select an activity");
        }
        if (result.hasErrors()) {
            model.addAttribute("activities", activityService.findAll());
            return "progress/form";
        }
        progress.setId(id);
        service.save(progress);
        attrs.addFlashAttribute("success", "Progress record updated successfully.");
        return "redirect:/progress";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes attrs) {
        service.deleteById(id);
        attrs.addFlashAttribute("success", "Progress record deleted successfully.");
        return "redirect:/progress";
    }
}
