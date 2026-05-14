package com.udjcs.rehearsal;

import com.udjcs.activity.ActivityService;
import com.udjcs.member.MemberService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/rehearsals")
public class RehearsalController {

    private final RehearsalService service;
    private final ActivityService activityService;
    private final MemberService memberService;

    public RehearsalController(RehearsalService service,
                               ActivityService activityService,
                               MemberService memberService) {
        this.service = service;
        this.activityService = activityService;
        this.memberService = memberService;
    }

    @GetMapping
    public String list(Model model) {
        Map<Long, List<Rehearsal>> groups = service.findGroupedByActivity();
        LocalDate today = LocalDate.now();

        Map<Long, Rehearsal> nextRehearsal = new LinkedHashMap<>();
        for (Map.Entry<Long, List<Rehearsal>> entry : groups.entrySet()) {
            entry.getValue().stream()
                    .filter(r -> !r.getRehearsalDate().isBefore(today))
                    .min(Comparator.comparing(Rehearsal::getRehearsalDate))
                    .ifPresent(r -> nextRehearsal.put(entry.getKey(), r));
        }

        List<Rehearsal> allRehearsals = groups.values().stream()
                .flatMap(List::stream).collect(Collectors.toList());
        Map<Long, Long> memberCounts  = service.memberCountByRehearsal(allRehearsals);
        Map<Long, Long> attendedCounts = service.attendedCountByRehearsal(allRehearsals);

        model.addAttribute("groups", groups);
        model.addAttribute("nextRehearsal", nextRehearsal);
        model.addAttribute("memberCounts", memberCounts);
        model.addAttribute("attendedCounts", attendedCounts);
        return "rehearsal/list";
    }

    @GetMapping("/new")
    public String showCreateForm(@RequestParam(required = false) Long activityId, Model model) {
        Rehearsal item = new Rehearsal();
        if (activityId != null) item.setActivityId(activityId);
        model.addAttribute("item", item);
        model.addAttribute("activities", activityService.findAll());
        model.addAttribute("members", memberService.findAll());
        model.addAttribute("existingMembers", List.of());
        return "rehearsal/form";
    }

    @PostMapping
    public String create(@ModelAttribute("item") @Valid Rehearsal rehearsal,
                         BindingResult result,
                         @RequestParam(value = "memberIds",   required = false) List<Long>   memberIds,
                         @RequestParam(value = "memberRoles", required = false) List<String> memberRoles,
                         Model model,
                         RedirectAttributes attrs) {
        if (rehearsal.getActivityId() == null)
            result.rejectValue("activityId", "NotNull", "Please select an activity");

        if (result.hasErrors()) {
            model.addAttribute("activities", activityService.findAll());
            model.addAttribute("members", memberService.findAll());
            model.addAttribute("existingMembers", List.of());
            return "rehearsal/form";
        }
        if (service.isDuplicate(rehearsal.getActivityId(), rehearsal.getRehearsalDate())) {
            model.addAttribute("duplicateError",
                "A rehearsal for this activity is already scheduled on " + rehearsal.getRehearsalDate() + ".");
            model.addAttribute("activities", activityService.findAll());
            model.addAttribute("members", memberService.findAll());
            model.addAttribute("existingMembers", List.of());
            return "rehearsal/form";
        }

        service.save(rehearsal);
        service.saveMembers(rehearsal.getId(), memberIds, memberRoles);
        attrs.addFlashAttribute("success", "Rehearsal saved successfully.");
        return "redirect:/rehearsals";
    }

    @GetMapping("/{id}")
    public String redirectToEdit(@PathVariable Long id) {
        return "redirect:/rehearsals/" + id + "/edit";
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model) {
        Rehearsal item = service.findById(id);
        item.setActivityId(item.getActivity().getId());
        model.addAttribute("item", item);
        model.addAttribute("activities", activityService.findAll());
        model.addAttribute("members", memberService.findAll());
        model.addAttribute("existingMembers", service.findMembers(id));
        return "rehearsal/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @ModelAttribute("item") @Valid Rehearsal rehearsal,
                         BindingResult result,
                         @RequestParam(value = "memberIds",   required = false) List<Long>   memberIds,
                         @RequestParam(value = "memberRoles", required = false) List<String> memberRoles,
                         Model model,
                         RedirectAttributes attrs) {
        if (rehearsal.getActivityId() == null)
            result.rejectValue("activityId", "NotNull", "Please select an activity");

        if (result.hasErrors()) {
            model.addAttribute("activities", activityService.findAll());
            model.addAttribute("members", memberService.findAll());
            model.addAttribute("existingMembers", service.findMembers(id));
            return "rehearsal/form";
        }
        if (service.isDuplicateExcluding(rehearsal.getActivityId(), rehearsal.getRehearsalDate(), id)) {
            model.addAttribute("duplicateError",
                "A rehearsal for this activity is already scheduled on " + rehearsal.getRehearsalDate() + ".");
            model.addAttribute("activities", activityService.findAll());
            model.addAttribute("members", memberService.findAll());
            model.addAttribute("existingMembers", service.findMembers(id));
            return "rehearsal/form";
        }

        rehearsal.setId(id);
        service.save(rehearsal);
        service.saveMembers(id, memberIds, memberRoles);
        attrs.addFlashAttribute("success", "Rehearsal updated successfully.");
        return "redirect:/rehearsals";
    }

    @GetMapping("/{id}/attendance")
    public String attendanceForm(@PathVariable Long id, Model model) {
        Rehearsal rehearsal = service.findById(id);
        List<RehearsalMember> rms = service.findMembers(id);
        Map<Long, String> memberNames = new LinkedHashMap<>();
        for (RehearsalMember rm : rms) {
            com.udjcs.member.Member m = memberService.findById(rm.getMemberId());
            memberNames.put(rm.getMemberId(), m.getFirstName() + " " + m.getLastName());
        }
        model.addAttribute("rehearsal", rehearsal);
        model.addAttribute("members", rms);
        model.addAttribute("memberNames", memberNames);
        return "rehearsal/attendance";
    }

    @PostMapping("/{id}/attendance")
    public String saveAttendance(@PathVariable Long id,
                                 @RequestParam(value = "attendedMemberIds", required = false) List<Long> attendedMemberIds,
                                 RedirectAttributes attrs) {
        service.saveAttendance(id, attendedMemberIds);
        attrs.addFlashAttribute("success", "Attendance saved successfully.");
        return "redirect:/rehearsals";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes attrs) {
        service.deleteById(id);
        attrs.addFlashAttribute("success", "Rehearsal deleted successfully.");
        return "redirect:/rehearsals";
    }
}
