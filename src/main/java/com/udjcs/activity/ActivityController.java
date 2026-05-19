package com.udjcs.activity;

import com.udjcs.activity.category.ActivityCategoryService;
import com.udjcs.event.EventRepository;
import com.udjcs.member.MemberService;
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
    private final MemberService memberService;
    private final EventRepository eventRepository;

    public ActivityController(ActivityService service, ActivityCategoryService categoryService,
                               MemberService memberService, EventRepository eventRepository) {
        this.service = service;
        this.categoryService = categoryService;
        this.memberService = memberService;
        this.eventRepository = eventRepository;
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
        model.addAttribute("members", memberService.findAll());
        model.addAttribute("events", eventRepository.findAll(org.springframework.data.domain.Sort.by("eventName")));
        return "activity/form";
    }

    @PostMapping
    public String create(@ModelAttribute("item") @Valid Activity activity,
                         BindingResult result, Model model, RedirectAttributes attrs) {
        if (activity.getCategoryId() == null) {
            result.rejectValue("categoryId", "NotNull", "Please select a category");
        }
        if (result.hasErrors()) {
            model.addAttribute("categories", categoryService.findAll());
            model.addAttribute("members", memberService.findAll());
            model.addAttribute("events", eventRepository.findAll(org.springframework.data.domain.Sort.by("eventName")));
            return "activity/form";
        }
        wireEvent(activity);
        service.save(activity);
        attrs.addFlashAttribute("success", "Activity saved successfully.");
        return "redirect:/activities";
    }

    @GetMapping("/{id}")
    public String redirectToEdit(@PathVariable Long id) {
        return "redirect:/activities/" + id + "/edit";
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model) {
        Activity item = service.findById(id);
        item.setCategoryId(item.getActivityCategory().getId());
        if (item.getEvent() != null) item.setEventId(item.getEvent().getId());
        model.addAttribute("item", item);
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("members", memberService.findAll());
        model.addAttribute("events", eventRepository.findAll(org.springframework.data.domain.Sort.by("eventName")));
        return "activity/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @ModelAttribute("item") @Valid Activity activity,
                         BindingResult result, Model model, RedirectAttributes attrs) {
        if (activity.getCategoryId() == null) {
            result.rejectValue("categoryId", "NotNull", "Please select a category");
        }
        if (result.hasErrors()) {
            model.addAttribute("categories", categoryService.findAll());
            model.addAttribute("members", memberService.findAll());
            model.addAttribute("events", eventRepository.findAll(org.springframework.data.domain.Sort.by("eventName")));
            return "activity/form";
        }
        activity.setId(id);
        wireEvent(activity);
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

    private void wireEvent(Activity a) {
        if (a.getEventId() != null) eventRepository.findById(a.getEventId()).ifPresent(a::setEvent);
        else a.setEvent(null);
    }
}
