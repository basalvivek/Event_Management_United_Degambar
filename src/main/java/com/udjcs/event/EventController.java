package com.udjcs.event;

import com.udjcs.supportive.SupportiveOrganizationService;
import com.udjcs.venue.VenueService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/events")
public class EventController {

    private final EventService service;
    private final SupportiveOrganizationService supportiveService;
    private final VenueService venueService;

    public EventController(EventService service, SupportiveOrganizationService supportiveService,
                           VenueService venueService) {
        this.service = service;
        this.supportiveService = supportiveService;
        this.venueService = venueService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("items", service.findAll());
        return "event/list";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("item", new Event());
        model.addAttribute("organizers", supportiveService.findAll());
        model.addAttribute("venues", venueService.findAll());

        return "event/form";
    }

    @PostMapping
    public String create(@ModelAttribute("item") @Valid Event event,
                         BindingResult result,
                         Model model,
                         RedirectAttributes attrs) {
        if (result.hasErrors()) {
            model.addAttribute("organizers", supportiveService.findAll());
            model.addAttribute("venues", venueService.findAll());

            return "event/form";
        }
        service.save(event);
        attrs.addFlashAttribute("success", "Event saved successfully.");
        return "redirect:/events";
    }

    @GetMapping("/{id}")
    public String redirectToEdit(@PathVariable Long id) {
        return "redirect:/events/" + id + "/edit";
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model) {
        model.addAttribute("item", service.findById(id));
        model.addAttribute("organizers", supportiveService.findAll());
        model.addAttribute("venues", venueService.findAll());

        return "event/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @ModelAttribute("item") @Valid Event event,
                         BindingResult result,
                         Model model,
                         RedirectAttributes attrs) {
        if (result.hasErrors()) {
            model.addAttribute("organizers", supportiveService.findAll());
            model.addAttribute("venues", venueService.findAll());

            return "event/form";
        }
        event.setId(id);
        service.save(event);
        attrs.addFlashAttribute("success", "Event updated successfully.");
        return "redirect:/events";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes attrs) {
        service.deleteById(id);
        attrs.addFlashAttribute("success", "Event deleted successfully.");
        return "redirect:/events";
    }
}
