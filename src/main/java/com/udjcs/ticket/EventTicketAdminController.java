package com.udjcs.ticket;

import com.udjcs.event.EventService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/ticket-payments")
public class EventTicketAdminController {

    private final EventTicketService service;
    private final EventService eventService;

    public EventTicketAdminController(EventTicketService service, EventService eventService) {
        this.service = service;
        this.eventService = eventService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) Long eventId,
                       @RequestParam(required = false) String status,
                       Model model) {
        java.util.List<EventTicket> tickets = service.findFiltered(eventId, status);
        java.util.List<EventTicket> allTickets = service.findFiltered(null, null);
        long totalPending  = allTickets.stream().filter(t -> "Pending".equals(t.getStatus())).count();
        long totalAccepted = allTickets.stream().filter(t -> "Accepted".equals(t.getStatus())).count();
        long totalRevenue  = allTickets.stream().filter(t -> "Accepted".equals(t.getStatus())).mapToInt(EventTicket::getTotalAmount).sum();

        // Group filtered tickets by event name, preserving event-date order
        java.util.LinkedHashMap<String, java.util.List<EventTicket>> grouped = new java.util.LinkedHashMap<>();
        for (EventTicket t : tickets) {
            String key = t.getEvent().getEventName();
            grouped.computeIfAbsent(key, k -> new java.util.ArrayList<>()).add(t);
        }
        // Per-event subtotals
        java.util.Map<String, Integer> eventTotals = new java.util.LinkedHashMap<>();
        grouped.forEach((name, list) ->
            eventTotals.put(name, list.stream().mapToInt(EventTicket::getTotalAmount).sum()));

        model.addAttribute("grouped", grouped);
        model.addAttribute("eventTotals", eventTotals);
        model.addAttribute("events", eventService.findAll());
        model.addAttribute("selectedEventId", eventId);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("totalShown", tickets.stream().mapToInt(EventTicket::getTotalAmount).sum());
        model.addAttribute("totalPending",  totalPending);
        model.addAttribute("totalAccepted", totalAccepted);
        model.addAttribute("totalRevenue",  totalRevenue);
        model.addAttribute("grandTotal",    allTickets.size());
        return "ticket/list";
    }

    @PostMapping("/{id}/approve")
    public String approve(@PathVariable Long id, RedirectAttributes attrs) {
        service.approve(id);
        attrs.addFlashAttribute("success", "Registration accepted. Email confirmation will be sent once email service is activated.");
        return "redirect:/ticket-payments";
    }

    @PostMapping("/{id}/reject")
    public String reject(@PathVariable Long id, RedirectAttributes attrs) {
        service.reject(id);
        attrs.addFlashAttribute("success", "Registration rejected.");
        return "redirect:/ticket-payments";
    }
}
