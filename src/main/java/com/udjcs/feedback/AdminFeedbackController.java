package com.udjcs.feedback;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/event-feedback")
public class AdminFeedbackController {

    private final EventFeedbackService service;

    public AdminFeedbackController(EventFeedbackService service) {
        this.service = service;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("items", service.findAll());
        return "feedback/list";
    }

    @PostMapping("/{id}/reply")
    public String reply(@PathVariable Long id,
                        @RequestParam String adminReply,
                        RedirectAttributes attrs) {
        if (adminReply == null || adminReply.isBlank()) {
            attrs.addFlashAttribute("error", "Reply cannot be empty.");
            return "redirect:/event-feedback";
        }
        service.saveReply(id, adminReply);
        attrs.addFlashAttribute("success", "Reply saved. Email to member will be sent when email service is activated.");
        return "redirect:/event-feedback";
    }
}
