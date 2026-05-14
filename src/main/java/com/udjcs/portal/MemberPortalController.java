package com.udjcs.portal;

import com.udjcs.activity.ActivityRepository;
import com.udjcs.event.EventRepository;
import com.udjcs.member.Member;
import com.udjcs.member.MemberService;
import com.udjcs.organization.Organization;
import com.udjcs.organization.OrganizationDisplayPictureService;
import com.udjcs.organization.OrganizationService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/portal")
public class MemberPortalController {

    private final OrganizationService organizationService;
    private final OrganizationDisplayPictureService displayPictureService;
    private final EventRepository eventRepository;
    private final ActivityRepository activityRepository;
    private final MemberService memberService;

    public MemberPortalController(OrganizationService organizationService,
                                   OrganizationDisplayPictureService displayPictureService,
                                   EventRepository eventRepository,
                                   ActivityRepository activityRepository,
                                   MemberService memberService) {
        this.organizationService = organizationService;
        this.displayPictureService = displayPictureService;
        this.eventRepository = eventRepository;
        this.activityRepository = activityRepository;
        this.memberService = memberService;
    }

    @ModelAttribute
    public void addOrgToModel(Model model) {
        List<Organization> orgs = organizationService.findAll();
        model.addAttribute("org", orgs.isEmpty() ? null : orgs.get(0));
    }

    @GetMapping
    public String home(Model model) {
        Organization org = (Organization) model.getAttribute("org");
        if (org != null) {
            model.addAttribute("displayPictures", displayPictureService.findByOrganization(org.getId()));
        } else {
            model.addAttribute("displayPictures", List.of());
        }
        LocalDate today = LocalDate.now();
        model.addAttribute("upcomingEvents",
                eventRepository.findTop5ByEventDateGreaterThanEqualAndStatusInOrderByEventDateAsc(
                        today, List.of("Planned", "Active")));
        model.addAttribute("activeActivities",
                activityRepository.findVisibleOnPortal(today));
        return "portal/home";
    }

    @PostMapping("/logout")
    public String logout(HttpSession session) {
        session.removeAttribute("memberLoggedIn");
        session.removeAttribute("memberUser");
        return "redirect:/member-login";
    }

    @GetMapping("/contact")
    public String contact() {
        return "portal/contact";
    }

    @GetMapping("/settings")
    public String settings(HttpSession session, Model model) {
        model.addAttribute("member", session.getAttribute("memberUser"));
        return "portal/settings";
    }

    @PostMapping("/settings")
    public String saveSettings(@RequestParam(required = false) String address,
                               @RequestParam(required = false) String phone,
                               @RequestParam(required = false) String email,
                               HttpSession session,
                               RedirectAttributes attrs) {
        if (address == null || address.isBlank()) {
            attrs.addFlashAttribute("error", "Address is required.");
            return "redirect:/portal/settings";
        }
        if (phone == null || phone.isBlank()) {
            attrs.addFlashAttribute("error", "Phone number is required.");
            return "redirect:/portal/settings";
        }
        if (email != null && !email.isBlank() && !email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            attrs.addFlashAttribute("error", "Enter a valid email address.");
            return "redirect:/portal/settings";
        }
        Member current = (Member) session.getAttribute("memberUser");
        Member updated = memberService.updateProfile(current.getId(), address.trim(), phone.trim(), email);
        session.setAttribute("memberUser", updated);
        attrs.addFlashAttribute("success", "Profile updated successfully.");
        return "redirect:/portal/settings";
    }

    @PostMapping("/settings/photo")
    public String uploadPhoto(@RequestParam("photoFile") MultipartFile file,
                              HttpSession session,
                              RedirectAttributes attrs) throws IOException {
        if (file == null || file.isEmpty()) {
            attrs.addFlashAttribute("error", "Please select an image.");
            return "redirect:/portal/settings";
        }
        if (file.getSize() > 2_097_152) {
            attrs.addFlashAttribute("error", "Photo must be 2 MB or smaller.");
            return "redirect:/portal/settings";
        }
        String ct = file.getContentType();
        if (ct == null || !ct.startsWith("image/")) {
            attrs.addFlashAttribute("error", "File must be an image.");
            return "redirect:/portal/settings";
        }
        Member current = (Member) session.getAttribute("memberUser");
        memberService.savePhoto(current.getId(), file);
        session.setAttribute("memberUser", memberService.findById(current.getId()));
        attrs.addFlashAttribute("success", "Profile photo updated.");
        return "redirect:/portal/settings";
    }

    @PostMapping("/settings/photo/delete")
    public String deletePhoto(HttpSession session, RedirectAttributes attrs) {
        Member current = (Member) session.getAttribute("memberUser");
        memberService.deletePhoto(current.getId());
        session.setAttribute("memberUser", memberService.findById(current.getId()));
        attrs.addFlashAttribute("success", "Profile photo removed.");
        return "redirect:/portal/settings";
    }
}
