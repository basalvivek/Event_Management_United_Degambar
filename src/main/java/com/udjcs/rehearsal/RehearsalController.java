package com.udjcs.rehearsal;

import com.udjcs.activity.ActivityService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/rehearsals")
public class RehearsalController {

    private final RehearsalService service;
    private final ActivityService activityService;

    public RehearsalController(RehearsalService service, ActivityService activityService) {
        this.service = service;
        this.activityService = activityService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("items", service.findAll());
        return "rehearsal/list";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("item", new Rehearsal());
        model.addAttribute("activities", activityService.findAll());
        return "rehearsal/form";
    }

    @PostMapping
    public String create(@ModelAttribute("item") @Valid Rehearsal rehearsal,
                         BindingResult result,
                         Model model,
                         RedirectAttributes attrs) {
        if (rehearsal.getActivityId() == null) {
            result.rejectValue("activityId", "NotNull", "Please select an activity");
        }
        if (result.hasErrors()) {
            model.addAttribute("activities", activityService.findAll());
            return "rehearsal/form";
        }
        service.save(rehearsal);
        attrs.addFlashAttribute("success", "Rehearsal saved successfully.");
        return "redirect:/rehearsals";
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model) {
        Rehearsal item = service.findById(id);
        item.setActivityId(item.getActivity().getId());
        model.addAttribute("item", item);
        model.addAttribute("activities", activityService.findAll());
        return "rehearsal/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @ModelAttribute("item") @Valid Rehearsal rehearsal,
                         BindingResult result,
                         Model model,
                         RedirectAttributes attrs) {
        if (rehearsal.getActivityId() == null) {
            result.rejectValue("activityId", "NotNull", "Please select an activity");
        }
        if (result.hasErrors()) {
            model.addAttribute("activities", activityService.findAll());
            return "rehearsal/form";
        }
        rehearsal.setId(id);
        service.save(rehearsal);
        attrs.addFlashAttribute("success", "Rehearsal updated successfully.");
        return "redirect:/rehearsals";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes attrs) {
        service.deleteById(id);
        attrs.addFlashAttribute("success", "Rehearsal deleted successfully.");
        return "redirect:/rehearsals";
    }
}
