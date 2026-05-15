package com.udjcs.eventprogram;

import com.udjcs.event.EventService;
import com.udjcs.member.MemberService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/event-programs")
public class EventProgramController {

    private final EventProgramService service;
    private final EventService eventService;
    private final MemberService memberService;

    public EventProgramController(EventProgramService service,
                                  EventService eventService,
                                  MemberService memberService) {
        this.service = service;
        this.eventService = eventService;
        this.memberService = memberService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("grouped", service.findGroupedByEvent());
        return "event-program/list";
    }

    @GetMapping("/event/{eventId}")
    public String viewByEvent(@PathVariable Long eventId, Model model) {
        model.addAttribute("event", eventService.findById(eventId));
        model.addAttribute("programs", service.findByEventId(eventId));
        return "event-program/view";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("item", null);
        model.addAttribute("events", eventService.findByStatus("Planned"));
        model.addAttribute("members", memberService.findAll());
        return "event-program/form";
    }

    @PostMapping
    public String create(@RequestParam(required = false) Long eventId,
                         @RequestParam(required = false) List<String> programNames,
                         @RequestParam(required = false) List<String> programDescriptions,
                         @RequestParam(required = false) List<String> startTimes,
                         @RequestParam(required = false) List<String> endTimes,
                         @RequestParam(required = false) List<String> responsibleMemberIds,
                         @RequestParam(required = false) List<String> remarks,
                         Model model,
                         RedirectAttributes attrs) {

        if (eventId == null)
            return errorForm(model, "Please select an event.");

        if (programNames == null || programNames.stream().allMatch(s -> s == null || s.isBlank()))
            return errorForm(model, "Please add at least one program row.");

        List<String> rowErrors = new ArrayList<>();
        List<EventProgram> toSave = new ArrayList<>();

        for (int i = 0; i < programNames.size(); i++) {
            String name = str(programNames, i);
            if (name == null) continue;

            String startStr = str(startTimes, i);
            String endStr   = str(endTimes, i);

            if (startStr == null || endStr == null) {
                rowErrors.add("Row " + (i + 1) + ": start and end time are required.");
                continue;
            }

            try {
                LocalTime start = LocalTime.parse(startStr);
                LocalTime end   = LocalTime.parse(endStr);

                if (!end.isAfter(start)) {
                    rowErrors.add("Row " + (i + 1) + ": end time must be after start time.");
                    continue;
                }

                EventProgram p = new EventProgram();
                p.setEventId(eventId);
                p.setProgramName(name);
                p.setProgramDescription(str(programDescriptions, i));
                p.setStartTime(start);
                p.setEndTime(end);
                String mid = str(responsibleMemberIds, i);
                if (mid != null) {
                    try { p.setResponsibleMemberId(Long.parseLong(mid)); }
                    catch (NumberFormatException ignored) {}
                }
                p.setRemark(str(remarks, i));
                toSave.add(p);

            } catch (DateTimeParseException e) {
                rowErrors.add("Row " + (i + 1) + ": invalid time format.");
            }
        }

        if (toSave.isEmpty())
            return errorForm(model, rowErrors.isEmpty()
                    ? "Please fill at least one complete program row."
                    : String.join(" ", rowErrors));

        service.saveAll(eventId, toSave);

        String msg = toSave.size() + " program" + (toSave.size() > 1 ? "s" : "") + " added successfully.";
        if (!rowErrors.isEmpty()) msg += " Skipped: " + String.join(" ", rowErrors);
        attrs.addFlashAttribute("success", msg);
        return "redirect:/event-programs";
    }

    @GetMapping("/{eventId}/edit-all")
    public String showEditAllForm(@PathVariable Long eventId, Model model) {
        model.addAttribute("event", eventService.findById(eventId));
        model.addAttribute("programs", service.findByEventId(eventId));
        model.addAttribute("members", memberService.findAll());
        return "event-program/edit-all";
    }

    @PostMapping("/{eventId}/edit-all")
    public String updateAll(@PathVariable Long eventId,
                            @RequestParam(required = false) List<String> programNames,
                            @RequestParam(required = false) List<String> programDescriptions,
                            @RequestParam(required = false) List<String> startTimes,
                            @RequestParam(required = false) List<String> endTimes,
                            @RequestParam(required = false) List<String> responsibleMemberIds,
                            @RequestParam(required = false) List<String> remarks,
                            Model model,
                            RedirectAttributes attrs) {

        if (programNames == null || programNames.stream().allMatch(s -> s == null || s.isBlank())) {
            model.addAttribute("event", eventService.findById(eventId));
            model.addAttribute("programs", service.findByEventId(eventId));
            model.addAttribute("members", memberService.findAll());
            model.addAttribute("formError", "Please keep at least one program.");
            return "event-program/edit-all";
        }

        List<String> rowErrors = new ArrayList<>();
        List<EventProgram> toSave = new ArrayList<>();

        for (int i = 0; i < programNames.size(); i++) {
            String name = str(programNames, i);
            if (name == null) continue;
            String startStr = str(startTimes, i);
            String endStr   = str(endTimes, i);
            if (startStr == null || endStr == null) {
                rowErrors.add("Row " + (i + 1) + ": start and end time are required.");
                continue;
            }
            try {
                LocalTime start = LocalTime.parse(startStr);
                LocalTime end   = LocalTime.parse(endStr);
                if (!end.isAfter(start)) {
                    rowErrors.add("Row " + (i + 1) + ": end time must be after start time.");
                    continue;
                }
                EventProgram p = new EventProgram();
                p.setEventId(eventId);
                p.setProgramName(name);
                p.setProgramDescription(str(programDescriptions, i));
                p.setStartTime(start);
                p.setEndTime(end);
                String mid = str(responsibleMemberIds, i);
                if (mid != null) {
                    try { p.setResponsibleMemberId(Long.parseLong(mid)); }
                    catch (NumberFormatException ignored) {}
                }
                p.setRemark(str(remarks, i));
                toSave.add(p);
            } catch (DateTimeParseException e) {
                rowErrors.add("Row " + (i + 1) + ": invalid time format.");
            }
        }

        if (toSave.isEmpty()) {
            model.addAttribute("event", eventService.findById(eventId));
            model.addAttribute("programs", service.findByEventId(eventId));
            model.addAttribute("members", memberService.findAll());
            model.addAttribute("formError", rowErrors.isEmpty()
                    ? "Please fill at least one complete program row."
                    : String.join(" ", rowErrors));
            return "event-program/edit-all";
        }

        service.replaceAll(eventId, toSave);

        String msg = "Programs updated successfully.";
        if (!rowErrors.isEmpty()) msg += " Skipped: " + String.join(" ", rowErrors);
        attrs.addFlashAttribute("success", msg);
        return "redirect:/event-programs/event/" + eventId;
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id,
                               @RequestParam(required = false) Long returnEventId,
                               Model model) {
        EventProgram item = service.findById(id);
        item.setEventId(item.getEvent().getId());
        if (item.getResponsibleMember() != null)
            item.setResponsibleMemberId(item.getResponsibleMember().getId());
        model.addAttribute("item", item);
        model.addAttribute("events", eventService.findAll());
        model.addAttribute("members", memberService.findAll());
        model.addAttribute("returnEventId", returnEventId);
        return "event-program/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @ModelAttribute("item") @Valid EventProgram program,
                         BindingResult result,
                         @RequestParam(required = false) Long returnEventId,
                         Model model, RedirectAttributes attrs) {
        if (program.getEventId() == null)
            result.rejectValue("eventId", "NotNull", "Please select an event");
        if (result.hasErrors()) {
            model.addAttribute("events", eventService.findAll());
            model.addAttribute("members", memberService.findAll());
            model.addAttribute("returnEventId", returnEventId);
            return "event-program/form";
        }
        program.setId(id);
        service.save(program);
        attrs.addFlashAttribute("success", "Event program updated successfully.");
        return returnEventId != null
                ? "redirect:/event-programs/event/" + returnEventId
                : "redirect:/event-programs";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id,
                         @RequestParam(required = false) Long returnEventId,
                         RedirectAttributes attrs) {
        service.deleteById(id);
        attrs.addFlashAttribute("success", "Event program deleted successfully.");
        return returnEventId != null
                ? "redirect:/event-programs/event/" + returnEventId
                : "redirect:/event-programs";
    }

    private String errorForm(Model model, String message) {
        model.addAttribute("item", null);
        model.addAttribute("formError", message);
        model.addAttribute("events", eventService.findByStatus("Planned"));
        model.addAttribute("members", memberService.findAll());
        return "event-program/form";
    }

    private String str(List<String> list, int i) {
        if (list == null || i >= list.size()) return null;
        String v = list.get(i);
        return (v != null && !v.isBlank()) ? v.trim() : null;
    }
}
