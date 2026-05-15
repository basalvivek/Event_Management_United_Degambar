package com.udjcs.announcement;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.time.LocalDate;

@Controller
@RequestMapping("/announcements")
public class AnnouncementController {

    private final AnnouncementService service;

    public AnnouncementController(AnnouncementService service) {
        this.service = service;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("items", service.findAll());
        model.addAttribute("today", LocalDate.now());
        return "announcement/list";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        Announcement a = new Announcement();
        a.setPublishedDate(LocalDate.now());
        a.setActive(true);
        model.addAttribute("item", a);
        return "announcement/form";
    }

    @PostMapping
    public String create(@ModelAttribute("item") @Valid Announcement announcement,
                         BindingResult result, RedirectAttributes attrs) {
        if (result.hasErrors()) return "announcement/form";
        service.save(announcement);
        attrs.addFlashAttribute("success", "Announcement created.");
        return "redirect:/announcements";
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model) {
        model.addAttribute("item", service.findById(id));
        return "announcement/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @ModelAttribute("item") @Valid Announcement announcement,
                         BindingResult result, RedirectAttributes attrs) {
        if (result.hasErrors()) return "announcement/form";
        announcement.setId(id);
        service.save(announcement);
        attrs.addFlashAttribute("success", "Announcement updated.");
        return "redirect:/announcements";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes attrs) {
        service.deleteById(id);
        attrs.addFlashAttribute("success", "Announcement deleted.");
        return "redirect:/announcements";
    }
}
