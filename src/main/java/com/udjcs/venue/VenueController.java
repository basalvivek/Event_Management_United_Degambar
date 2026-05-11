package com.udjcs.venue;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/venues")
public class VenueController {

    private final VenueService service;

    public VenueController(VenueService service) {
        this.service = service;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("items", service.findAll());
        return "venue/list";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("item", new Venue());
        return "venue/form";
    }

    @PostMapping
    public String create(@ModelAttribute("item") @Valid Venue venue,
                         BindingResult result,
                         RedirectAttributes attrs) {
        if (result.hasErrors()) return "venue/form";
        service.save(venue);
        attrs.addFlashAttribute("success", "Venue saved successfully.");
        return "redirect:/venues";
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model) {
        model.addAttribute("item", service.findById(id));
        return "venue/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @ModelAttribute("item") @Valid Venue venue,
                         BindingResult result,
                         RedirectAttributes attrs) {
        if (result.hasErrors()) return "venue/form";
        venue.setId(id);
        service.save(venue);
        attrs.addFlashAttribute("success", "Venue updated successfully.");
        return "redirect:/venues";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes attrs) {
        service.deleteById(id);
        attrs.addFlashAttribute("success", "Venue deleted successfully.");
        return "redirect:/venues";
    }
}
