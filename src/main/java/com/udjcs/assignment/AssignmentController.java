package com.udjcs.assignment;

import com.udjcs.activity.ActivityService;
import com.udjcs.member.MemberService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/assignments")
public class AssignmentController {

    private final AssignmentService service;
    private final ActivityService activityService;
    private final MemberService memberService;

    public AssignmentController(AssignmentService service,
                                ActivityService activityService,
                                MemberService memberService) {
        this.service = service;
        this.activityService = activityService;
        this.memberService = memberService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("items", service.findAll());
        return "assignment/list";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("item", new Assignment());
        addFormData(model);
        return "assignment/form";
    }

    @PostMapping
    public String create(@ModelAttribute("item") @Valid Assignment assignment,
                         BindingResult result,
                         Model model,
                         RedirectAttributes attrs) {
        rejectIfMissingFks(assignment, result);
        if (result.hasErrors()) {
            addFormData(model);
            return "assignment/form";
        }
        service.save(assignment);
        attrs.addFlashAttribute("success", "Assignment saved successfully.");
        return "redirect:/assignments";
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model) {
        Assignment item = service.findById(id);
        item.setActivityId(item.getActivity().getId());
        item.setMemberId(item.getMember().getId());
        model.addAttribute("item", item);
        addFormData(model);
        return "assignment/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @ModelAttribute("item") @Valid Assignment assignment,
                         BindingResult result,
                         Model model,
                         RedirectAttributes attrs) {
        rejectIfMissingFks(assignment, result);
        if (result.hasErrors()) {
            addFormData(model);
            return "assignment/form";
        }
        assignment.setId(id);
        service.save(assignment);
        attrs.addFlashAttribute("success", "Assignment updated successfully.");
        return "redirect:/assignments";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes attrs) {
        service.deleteById(id);
        attrs.addFlashAttribute("success", "Assignment deleted successfully.");
        return "redirect:/assignments";
    }

    private void addFormData(Model model) {
        model.addAttribute("activities", activityService.findAll());
        model.addAttribute("members", memberService.findAll());
    }

    private void rejectIfMissingFks(Assignment assignment, BindingResult result) {
        if (assignment.getActivityId() == null) {
            result.rejectValue("activityId", "NotNull", "Please select an activity");
        }
        if (assignment.getMemberId() == null) {
            result.rejectValue("memberId", "NotNull", "Please select a member");
        }
    }
}
