package com.udjcs.assignment;

import com.udjcs.activity.ActivityService;
import com.udjcs.member.MemberService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

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
        java.util.Map<Long, java.util.List<Assignment>> groups = service.findGroupedByActivity();
        int totalAssignments = groups.values().stream().mapToInt(java.util.List::size).sum();
        long withLead = groups.values().stream()
                .filter(list -> list.stream().anyMatch(a -> "Lead".equals(a.getRole())))
                .count();
        model.addAttribute("groups", groups);
        model.addAttribute("totalAssignments", totalAssignments);
        model.addAttribute("activitiesWithLead", withLead);
        return "assignment/list";
    }

    @GetMapping("/new")
    public String showCreateForm(@RequestParam(required = false) Long activityId, Model model) {
        Assignment item = new Assignment();
        if (activityId != null) item.setActivityId(activityId);
        model.addAttribute("item", item);
        addFormData(model);
        return "assignment/form";
    }

    @PostMapping
    public String create(@ModelAttribute("item") Assignment assignment,
                         BindingResult result,
                         @RequestParam(value = "memberIds",   required = false) List<Long>   memberIds,
                         @RequestParam(value = "memberRoles", required = false) List<String> memberRoles,
                         Model model,
                         RedirectAttributes attrs) {

        if (assignment.getActivityId() == null)
            result.rejectValue("activityId",   "NotNull",   "Please select an activity");
        if (assignment.getAssignedDate() == null)
            result.rejectValue("assignedDate",  "NotNull",   "Please enter an assigned date");
        if (assignment.getStatus() == null || assignment.getStatus().isBlank())
            result.rejectValue("status",        "NotBlank",  "Please select a status");
        if (memberIds == null || memberIds.isEmpty())
            result.rejectValue("memberId",      "NotNull",   "Please select at least one member");

        if (result.hasErrors()) {
            addFormData(model);
            return "assignment/form";
        }

        // Collect duplicates first — show on form before saving anything
        List<String> duplicateNames = new java.util.ArrayList<>();
        for (int i = 0; i < memberIds.size(); i++) {
            Long memberId = memberIds.get(i);
            if (memberId == null) continue;
            if (service.isDuplicate(assignment.getActivityId(), memberId)) {
                String name = memberService.findById(memberId).getFirstName()
                            + " " + memberService.findById(memberId).getLastName();
                duplicateNames.add(name);
            }
        }
        if (!duplicateNames.isEmpty()) {
            model.addAttribute("duplicateError",
                "Already assigned to this activity: " + String.join(", ", duplicateNames));
            addFormData(model);
            return "assignment/form";
        }

        int saved = 0;
        for (int i = 0; i < memberIds.size(); i++) {
            Long memberId = memberIds.get(i);
            if (memberId == null) continue;
            Assignment a = new Assignment();
            a.setActivityId(assignment.getActivityId());
            a.setMemberId(memberId);
            a.setAssignedDate(assignment.getAssignedDate());
            a.setRole(memberRoles != null && i < memberRoles.size() ? memberRoles.get(i) : "Volunteer");
            a.setStatus(assignment.getStatus());
            a.setNotes(assignment.getNotes());
            service.save(a);
            saved++;
        }
        attrs.addFlashAttribute("success",
            saved + " member" + (saved > 1 ? "s" : "") + " assigned successfully.");
        return "redirect:/assignments";
    }

    @GetMapping("/{id}")
    public String redirectToEdit(@PathVariable Long id) {
        return "redirect:/assignments/" + id + "/edit";
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
        if (assignment.getActivityId() == null)
            result.rejectValue("activityId", "NotNull", "Please select an activity");
        if (assignment.getMemberId() == null)
            result.rejectValue("memberId",   "NotNull", "Please select a member");
        if (result.hasErrors()) {
            addFormData(model);
            return "assignment/form";
        }
        // Check duplicate only if member changed
        Assignment existing = service.findById(id);
        if (!existing.getMember().getId().equals(assignment.getMemberId())
                && service.isDuplicate(assignment.getActivityId(), assignment.getMemberId())) {
            result.rejectValue("memberId", "Duplicate", "This member is already assigned to this activity.");
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
        model.addAttribute("members",    memberService.findAll());
    }
}
