package com.udjcs.ticket;

import com.udjcs.event.Event;
import com.udjcs.event.EventRepository;
import com.udjcs.member.Member;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/portal/register-event")
public class EventTicketPortalController {

    private final EventTicketService service;
    private final EventRepository eventRepository;

    public EventTicketPortalController(EventTicketService service, EventRepository eventRepository) {
        this.service = service;
        this.eventRepository = eventRepository;
    }

    @GetMapping("/{eventId}")
    public String showForm(@PathVariable Long eventId, HttpSession session, Model model,
                           RedirectAttributes attrs) {
        Member member = (Member) session.getAttribute("memberUser");
        if (service.isRegistered(eventId, member.getId())) {
            attrs.addFlashAttribute("error", "You have already registered for this event.");
            return "redirect:/portal";
        }
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found: " + eventId));
        model.addAttribute("event", event);
        model.addAttribute("member", member);
        return "portal/register-event";
    }

    @PostMapping
    public String register(@RequestParam Long eventId,
                           @RequestParam(required = false, defaultValue = "0") Integer adultCount,
                           @RequestParam(required = false, defaultValue = "0") Integer youngerCount,
                           @RequestParam(required = false, defaultValue = "0") Integer childCount,
                           HttpSession session,
                           RedirectAttributes attrs) {
        Member member = (Member) session.getAttribute("memberUser");

        if (adultCount < 0 || youngerCount < 0 || childCount < 0) {
            attrs.addFlashAttribute("error", "Attendee counts cannot be negative.");
            return "redirect:/portal/register-event/" + eventId;
        }
        if (service.isRegistered(eventId, member.getId())) {
            attrs.addFlashAttribute("error", "You have already registered for this event.");
            return "redirect:/portal";
        }
        if (adultCount + youngerCount + childCount == 0) {
            attrs.addFlashAttribute("error", "Please add at least one attendee.");
            return "redirect:/portal/register-event/" + eventId;
        }

        service.register(eventId, member.getId(), adultCount, youngerCount, childCount);
        attrs.addFlashAttribute("success", "Registration submitted successfully! Your booking is pending confirmation.");
        return "redirect:/portal";
    }
}
