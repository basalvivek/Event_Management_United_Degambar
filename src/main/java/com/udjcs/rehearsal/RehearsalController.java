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
    private final RehearsalRepository rehearsalRepo;

    public RehearsalController(RehearsalService service,
                               ActivityService activityService,
                               MemberService memberService,
                               RehearsalRepository rehearsalRepo) {
        this.service         = service;
        this.activityService = activityService;
        this.memberService   = memberService;
        this.rehearsalRepo   = rehearsalRepo;
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

        Map<Long, List<Object[]>> memberDetails = service.memberDetailsByRehearsal(allRehearsals);

        model.addAttribute("groups", groups);
        model.addAttribute("nextRehearsal", nextRehearsal);
        model.addAttribute("memberCounts", memberCounts);
        model.addAttribute("attendedCounts", attendedCounts);
        model.addAttribute("memberDetails", memberDetails);
        return "rehearsal/list";
    }

    @GetMapping("/new")
    public String showCreateForm(@RequestParam(required = false) Long activityId, Model model) {
        Rehearsal item = new Rehearsal();
        item.setStatus("Scheduled");
        if (activityId != null) item.setActivityId(activityId);
        model.addAttribute("item", item);
        model.addAttribute("activities", activityService.findAll());
        model.addAttribute("members", memberService.findAll());
        model.addAttribute("existingMembers", List.of());
        model.addAttribute("memberNameMap", Map.of());
        model.addAttribute("preselectedActivity", null);
        model.addAttribute("moreSession", false);
        model.addAttribute("addMembersMode", false);
        return "rehearsal/form";
    }

    @GetMapping("/more/{activityId}")
    public String moreSession(@PathVariable Long activityId,
                               Model model,
                               RedirectAttributes attrs) {
        List<Rehearsal> past = rehearsalRepo.findByActivityIdOrderByDateDesc(activityId);
        Rehearsal item = new Rehearsal();
        item.setActivityId(activityId);
        item.setStatus("Scheduled");
        java.util.List<RehearsalMember> existingMembers = List.of();
        if (!past.isEmpty()) {
            Rehearsal src = past.get(0);
            if (!"Completed".equals(src.getStatus()) && !"Cancelled".equals(src.getStatus())) {
                src.setStatus("Completed");
                rehearsalRepo.save(src);
            }
            // Carry over all fields from last session
            item.setRehearsalDate(src.getRehearsalDate());
            item.setStartTime(src.getStartTime());
            item.setEndTime(src.getEndTime());
            item.setVenue(src.getVenue());
            item.setConductedBy(src.getConductedBy());
            item.setNotes(src.getNotes());
            existingMembers = service.findMembers(src.getId());
        }
        Map<Long, String> memberNameMap = new LinkedHashMap<>();
        for (RehearsalMember rm : existingMembers) {
            try {
                com.udjcs.member.Member m = memberService.findById(rm.getMemberId());
                memberNameMap.put(rm.getMemberId(), m.getFirstName() + " " + m.getLastName());
            } catch (Exception ignored) {}
        }
        model.addAttribute("item", item);
        model.addAttribute("preselectedActivity", activityService.findById(activityId));
        model.addAttribute("activities", activityService.findAll());
        model.addAttribute("members", memberService.findAll());
        model.addAttribute("existingMembers", existingMembers);
        model.addAttribute("memberNameMap", memberNameMap);
        model.addAttribute("moreSession", true);
        model.addAttribute("addMembersMode", false);
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
            model.addAttribute("memberNameMap", Map.of());
            model.addAttribute("preselectedActivity", null);
            model.addAttribute("moreSession", false);
            model.addAttribute("addMembersMode", false);
            return "rehearsal/form";
        }
        if (service.isDuplicate(rehearsal.getActivityId(), rehearsal.getRehearsalDate())) {
            model.addAttribute("duplicateError",
                "A rehearsal for this activity is already scheduled on " + rehearsal.getRehearsalDate() + ".");
            model.addAttribute("activities", activityService.findAll());
            model.addAttribute("members", memberService.findAll());
            model.addAttribute("existingMembers", List.of());
            model.addAttribute("memberNameMap", Map.of());
            model.addAttribute("preselectedActivity", null);
            model.addAttribute("moreSession", false);
            model.addAttribute("addMembersMode", false);
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
        model.addAttribute("memberNameMap", Map.of());
        model.addAttribute("preselectedActivity", null);
        model.addAttribute("moreSession", false);
        model.addAttribute("addMembersMode", false);
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
            model.addAttribute("memberNameMap", Map.of());
            model.addAttribute("preselectedActivity", null);
            model.addAttribute("moreSession", false);
            model.addAttribute("addMembersMode", false);
            return "rehearsal/form";
        }
        if (service.isDuplicateExcluding(rehearsal.getActivityId(), rehearsal.getRehearsalDate(), id)) {
            model.addAttribute("duplicateError",
                "A rehearsal for this activity is already scheduled on " + rehearsal.getRehearsalDate() + ".");
            model.addAttribute("activities", activityService.findAll());
            model.addAttribute("members", memberService.findAll());
            model.addAttribute("existingMembers", service.findMembers(id));
            model.addAttribute("memberNameMap", Map.of());
            model.addAttribute("preselectedActivity", null);
            model.addAttribute("moreSession", false);
            model.addAttribute("addMembersMode", false);
            return "rehearsal/form";
        }

        rehearsal.setId(id);
        service.save(rehearsal);
        service.saveMembers(id, memberIds, memberRoles);
        attrs.addFlashAttribute("success", "Rehearsal updated successfully.");
        return "redirect:/rehearsals";
    }

    @GetMapping("/{id}/add-members")
    public String showAddMembersForm(@PathVariable Long id, Model model) {
        Rehearsal rehearsal = service.findById(id);
        List<RehearsalMember> existing = service.findMembers(id);
        Map<Long, String> memberNameMap = new LinkedHashMap<>();
        for (RehearsalMember rm : existing) {
            try {
                com.udjcs.member.Member m = memberService.findById(rm.getMemberId());
                memberNameMap.put(rm.getMemberId(), m.getFirstName() + " " + m.getLastName());
            } catch (Exception ignored) {}
        }
        model.addAttribute("rehearsal", rehearsal);
        model.addAttribute("existingMembers", existing);
        model.addAttribute("memberNameMap", memberNameMap);
        model.addAttribute("members", memberService.findAll());
        return "rehearsal/add-members";
    }

    @PostMapping("/{id}/add-members")
    public String addMembers(@PathVariable Long id,
                             @RequestParam(value = "memberIds",   required = false) List<Long>   memberIds,
                             @RequestParam(value = "memberRoles", required = false) List<String> memberRoles,
                             RedirectAttributes attrs) {
        service.appendMembers(id, memberIds, memberRoles);
        attrs.addFlashAttribute("success", "Members added successfully.");
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
