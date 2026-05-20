package com.udjcs.activity;

import com.udjcs.activity.category.ActivityCategoryService;
import com.udjcs.assignment.Assignment;
import com.udjcs.assignment.AssignmentRepository;
import com.udjcs.event.EventRepository;
import com.udjcs.member.MemberService;
import com.udjcs.rehearsal.Rehearsal;
import com.udjcs.rehearsal.RehearsalMemberRepository;
import com.udjcs.rehearsal.RehearsalRepository;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/activities")
public class ActivityController {

    private final ActivityService service;
    private final ActivityCategoryService categoryService;
    private final MemberService memberService;
    private final EventRepository eventRepository;
    private final AssignmentRepository assignmentRepository;
    private final RehearsalRepository rehearsalRepository;
    private final RehearsalMemberRepository rehearsalMemberRepository;

    public ActivityController(ActivityService service, ActivityCategoryService categoryService,
                               MemberService memberService, EventRepository eventRepository,
                               AssignmentRepository assignmentRepository,
                               RehearsalRepository rehearsalRepository,
                               RehearsalMemberRepository rehearsalMemberRepository) {
        this.service = service;
        this.categoryService = categoryService;
        this.memberService = memberService;
        this.eventRepository = eventRepository;
        this.assignmentRepository = assignmentRepository;
        this.rehearsalRepository = rehearsalRepository;
        this.rehearsalMemberRepository = rehearsalMemberRepository;
    }

    @GetMapping
    public String list(Model model) {
        List<Activity> activities = service.findAll();
        model.addAttribute("items", activities);

        // Assignments grouped by activity id
        Map<Long, List<Assignment>> assignMap = new HashMap<>();
        if (!activities.isEmpty()) {
            List<Long> ids = activities.stream().map(Activity::getId).collect(Collectors.toList());
            for (Assignment a : assignmentRepository.findByActivityIds(ids)) {
                assignMap.computeIfAbsent(a.getActivity().getId(), k -> new ArrayList<>()).add(a);
            }
        }
        model.addAttribute("assignMap", assignMap);

        // Rehearsal stats: total & completed per activity → pre-compute pct
        Map<Long, Integer> rehTotal = new HashMap<>();
        Map<Long, Integer> rehDone  = new HashMap<>();
        Map<Long, Integer> rehPct   = new HashMap<>();
        for (Object[] row : rehearsalRepository.countAllByActivity())
            rehTotal.put((Long) row[0], ((Number) row[1]).intValue());
        for (Object[] row : rehearsalRepository.countCompletedByActivity())
            rehDone.put((Long) row[0], ((Number) row[1]).intValue());
        for (Activity a : activities) {
            int total = rehTotal.getOrDefault(a.getId(), 0);
            int done  = rehDone.getOrDefault(a.getId(), 0);
            rehPct.put(a.getId(), total > 0 ? done * 100 / total : 0);
        }
        model.addAttribute("rehTotal", rehTotal);
        model.addAttribute("rehDone",  rehDone);
        model.addAttribute("rehPct",   rehPct);
        model.addAttribute("members",  memberService.findAll());

        // Rehearsal rows per activity (sorted ascending by date)
        Map<Long, List<Rehearsal>> rehRows = new HashMap<>();
        List<Rehearsal> allRehearsals = rehearsalRepository.findAllWithDetails();
        for (Rehearsal r : allRehearsals) {
            rehRows.computeIfAbsent(r.getActivity().getId(), k -> new ArrayList<>()).add(r);
        }
        rehRows.values().forEach(list ->
            list.sort(java.util.Comparator.comparing(Rehearsal::getRehearsalDate)));
        model.addAttribute("rehRows", rehRows);

        // Members per rehearsal (batch load: [rehearsalId, firstName, lastName, role, attended])
        List<Long> rehearsalIds = allRehearsals.stream()
            .map(r -> r.getId()).collect(Collectors.toList());
        Map<Long, List<Object[]>> rehMemberMap = new HashMap<>();
        if (!rehearsalIds.isEmpty()) {
            for (Object[] row : rehearsalMemberRepository.findAllMembersForRehearsalIds(rehearsalIds)) {
                Long rid = (Long) row[0];
                rehMemberMap.computeIfAbsent(rid, k -> new ArrayList<>()).add(row);
            }
        }
        model.addAttribute("rehMemberMap", rehMemberMap);

        // Member counts per rehearsal: total & attended
        Map<Long, Integer> rehTotalMbr = new HashMap<>();
        Map<Long, Integer> rehAttMbr   = new HashMap<>();
        for (Map.Entry<Long, List<Object[]>> e : rehMemberMap.entrySet()) {
            Long rid = e.getKey();
            List<Object[]> rows = e.getValue();
            rehTotalMbr.put(rid, rows.size());
            int att = (int) rows.stream().filter(row -> Boolean.TRUE.equals(row[4])).count();
            rehAttMbr.put(rid, att);
        }
        model.addAttribute("rehTotalMbr", rehTotalMbr);
        model.addAttribute("rehAttMbr",   rehAttMbr);

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
