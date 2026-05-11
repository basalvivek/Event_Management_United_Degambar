package com.udjcs.participation;

import com.udjcs.event.EventService;
import com.udjcs.member.MemberService;
import com.udjcs.supportive.SupportiveOrganizationService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/participations")
public class ParticipationController {

    private final ParticipationService service;
    private final EventService eventService;
    private final SupportiveOrganizationService organizationService;
    private final MemberService memberService;

    public ParticipationController(ParticipationService service,
                                   EventService eventService,
                                   SupportiveOrganizationService organizationService,
                                   MemberService memberService) {
        this.service = service;
        this.eventService = eventService;
        this.organizationService = organizationService;
        this.memberService = memberService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("items", service.findAll());
        return "participation/list";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("item", new Participation());
        addFormData(model);
        return "participation/form";
    }

    @PostMapping
    public String create(@ModelAttribute("item") @Valid Participation participation,
                         BindingResult result,
                         Model model,
                         RedirectAttributes attrs) {
        rejectIfMissingFks(participation, result);
        if (result.hasErrors()) {
            addFormData(model);
            return "participation/form";
        }
        service.save(participation);
        attrs.addFlashAttribute("success", "Participation saved successfully.");
        return "redirect:/participations";
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model) {
        Participation item = service.findById(id);
        item.setEventId(item.getEvent().getId());
        item.setOrganizationId(item.getSupportiveOrganization().getId());
        item.setMemberIds(item.getMembers().stream()
            .map(m -> m.getId())
            .collect(Collectors.toSet()));
        model.addAttribute("item", item);
        addFormData(model);
        return "participation/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @ModelAttribute("item") @Valid Participation participation,
                         BindingResult result,
                         Model model,
                         RedirectAttributes attrs) {
        rejectIfMissingFks(participation, result);
        if (result.hasErrors()) {
            addFormData(model);
            return "participation/form";
        }
        participation.setId(id);
        service.save(participation);
        attrs.addFlashAttribute("success", "Participation updated successfully.");
        return "redirect:/participations";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes attrs) {
        service.deleteById(id);
        attrs.addFlashAttribute("success", "Participation deleted successfully.");
        return "redirect:/participations";
    }

    private void addFormData(Model model) {
        model.addAttribute("events", eventService.findAll());
        model.addAttribute("organizations", organizationService.findAll());
        model.addAttribute("memberList", memberService.findAll());
    }

    private void rejectIfMissingFks(Participation participation, BindingResult result) {
        if (participation.getEventId() == null) {
            result.rejectValue("eventId", "NotNull", "Please select an event");
        }
        if (participation.getOrganizationId() == null) {
            result.rejectValue("organizationId", "NotNull", "Please select an organization");
        }
    }
}
