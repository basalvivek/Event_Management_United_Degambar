package com.udjcs.contact;

import com.udjcs.organization.Organization;
import com.udjcs.organization.OrganizationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/contact")
public class ContactController {

    private final OrganizationService organizationService;
    private final JavaMailSender mailSender;

    public ContactController(OrganizationService organizationService,
                             @Autowired(required = false) JavaMailSender mailSender) {
        this.organizationService = organizationService;
        this.mailSender = mailSender;
    }

    @GetMapping
    public String showForm(Model model) {
        List<Organization> orgs = organizationService.findAll();
        model.addAttribute("orgName",  orgs.isEmpty() ? "United Digambar Jain Community" : orgs.get(0).getName());
        model.addAttribute("orgEmail", orgs.isEmpty() ? null : orgs.get(0).getEmail());
        return "contact/index";
    }

    @PostMapping
    public String send(@RequestParam String senderName,
                       @RequestParam String senderEmail,
                       @RequestParam String subject,
                       @RequestParam String message,
                       RedirectAttributes attrs) {

        List<Organization> orgs = organizationService.findAll();
        if (orgs.isEmpty() || orgs.get(0).getEmail() == null || orgs.get(0).getEmail().isBlank()) {
            attrs.addFlashAttribute("error", "No recipient email configured in Organisation Settings.");
            return "redirect:/contact";
        }

        if (mailSender == null) {
            attrs.addFlashAttribute("error", "Mail service is not configured. Contact the administrator.");
            return "redirect:/contact";
        }

        try {
            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setFrom("basalvivek@gmail.com");
            mail.setTo(orgs.get(0).getEmail());
            mail.setReplyTo(senderEmail);
            mail.setSubject("[UDJCS] " + subject);
            mail.setText(
                "Name    : " + senderName + "\n" +
                "Email   : " + senderEmail + "\n\n" +
                message
            );
            mailSender.send(mail);
            attrs.addFlashAttribute("success", "Message sent successfully.");
        } catch (MailException e) {
            attrs.addFlashAttribute("error", "Failed to send message. Please check mail configuration.");
        }

        return "redirect:/contact";
    }
}
