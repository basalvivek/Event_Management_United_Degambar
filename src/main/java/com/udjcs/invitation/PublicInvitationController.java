package com.udjcs.invitation;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;

@Controller
@RequestMapping("/e")
public class PublicInvitationController {

    private final InvitationService service;
    private final InvitationRegistrationService registrationService;

    public PublicInvitationController(InvitationService service,
                                       InvitationRegistrationService registrationService) {
        this.service = service;
        this.registrationService = registrationService;
    }

    @GetMapping("/{slug}")
    public String view(@PathVariable String slug,
                       @RequestParam(required = false) Boolean registered,
                       Model model) {
        model.addAttribute("invitation", service.findBySlug(slug));
        model.addAttribute("registration", new InvitationRegistration());
        model.addAttribute("registered", Boolean.TRUE.equals(registered));
        return "invitation/public";
    }

    @PostMapping("/{slug}/register")
    public String register(@PathVariable String slug,
                           @ModelAttribute("registration") @Valid InvitationRegistration reg,
                           BindingResult result,
                           Model model,
                           RedirectAttributes attrs) {
        if (result.hasErrors()) {
            model.addAttribute("invitation", service.findBySlug(slug));
            model.addAttribute("registered", false);
            return "invitation/public";
        }
        registrationService.register(slug, reg);
        attrs.addFlashAttribute("registered", true);
        return "redirect:/e/" + slug + "?registered=true";
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public void handleNotFound(HttpServletResponse response) throws IOException {
        response.setStatus(404);
        response.setContentType("text/html;charset=UTF-8");
        response.getWriter().write(
            "<!DOCTYPE html><html><head><meta charset='UTF-8'>" +
            "<meta name='viewport' content='width=device-width,initial-scale=1'>" +
            "<title>Invitation Not Found</title>" +
            "<link rel='stylesheet' href='https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css'>" +
            "</head><body class='d-flex align-items-center justify-content-center' style='min-height:100vh;background:#f8f7ff;'>" +
            "<div class='text-center p-4'><div style='font-size:4rem;'>📨</div>" +
            "<h4 class='mt-3 fw-bold' style='color:#1e1b4b;'>Invitation Not Found</h4>" +
            "<p class='text-muted'>This invitation link may have expired or the URL is incorrect.</p>" +
            "<p class='text-muted small'>Please check the link and try again, or contact the organiser.</p>" +
            "</div></body></html>"
        );
    }
}
