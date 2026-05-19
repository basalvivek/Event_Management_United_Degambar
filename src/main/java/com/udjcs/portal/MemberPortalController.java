package com.udjcs.portal;

import com.udjcs.activity.ActivityRepository;
import com.udjcs.announcement.AnnouncementService;
import com.udjcs.event.EventRepository;
import com.udjcs.event.Event;
import com.udjcs.eventprogram.EventProgramService;
import com.udjcs.feedback.EventFeedbackService;
import com.udjcs.member.Member;
import com.udjcs.member.MemberRepository;
import com.udjcs.ticket.EventTicketService;
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
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/portal")
public class MemberPortalController {

    private final OrganizationService organizationService;
    private final OrganizationDisplayPictureService displayPictureService;
    private final EventRepository eventRepository;
    private final ActivityRepository activityRepository;
    private final MemberService memberService;
    private final MemberRepository memberRepository;
    private final EventProgramService eventProgramService;
    private final EventFeedbackService feedbackService;
    private final EventTicketService ticketService;
    private final AnnouncementService announcementService;

    public MemberPortalController(OrganizationService organizationService,
                                   OrganizationDisplayPictureService displayPictureService,
                                   EventRepository eventRepository,
                                   ActivityRepository activityRepository,
                                   MemberService memberService,
                                   MemberRepository memberRepository,
                                   EventProgramService eventProgramService,
                                   EventFeedbackService feedbackService,
                                   EventTicketService ticketService,
                                   AnnouncementService announcementService) {
        this.organizationService = organizationService;
        this.displayPictureService = displayPictureService;
        this.eventRepository = eventRepository;
        this.activityRepository = activityRepository;
        this.memberService = memberService;
        this.memberRepository = memberRepository;
        this.eventProgramService = eventProgramService;
        this.feedbackService = feedbackService;
        this.ticketService = ticketService;
        this.announcementService = announcementService;
    }

    @ModelAttribute
    public void addOrgToModel(Model model) {
        List<Organization> orgs = organizationService.findAll();
        model.addAttribute("org", orgs.isEmpty() ? null : orgs.get(0));
    }

    @GetMapping
    public String home(Model model, HttpSession session) {
        Organization org = (Organization) model.getAttribute("org");
        if (org != null) {
            model.addAttribute("displayPictures", displayPictureService.findByOrganization(org.getId()));
        } else {
            model.addAttribute("displayPictures", List.of());
        }

        // Events by status
        List<Event> activeEvents = eventRepository.findByStatusOrderByEventDateAsc("Active");
        model.addAttribute("activeEvents", activeEvents);
        model.addAttribute("plannedEvents",
                eventRepository.findByStatusOrderByEventDateAsc("Planned"));
        List<Event> completedEvents = eventRepository.findByStatusOrderByEventDateDesc("Completed");
        model.addAttribute("completedEvents", completedEvents);

        // Programs for planned and completed events
        List<com.udjcs.eventprogram.EventProgram> plannedProgs = eventProgramService.findByEventStatus("Planned");
        model.addAttribute("plannedProgramsList", plannedProgs);
        model.addAttribute("completedProgramsList",
                eventProgramService.findByEventStatus("Completed"));

        // Set of event IDs that have at least one planned program (for per-event "no programs" check)
        Set<Long> plannedEventIdsWithPrograms = new HashSet<>();
        for (com.udjcs.eventprogram.EventProgram p : plannedProgs) {
            plannedEventIdsWithPrograms.add(p.getEvent().getId());
        }
        model.addAttribute("plannedEventIdsWithPrograms", plannedEventIdsWithPrograms);

        // Track which completed events this member already gave feedback for
        Member member = (Member) session.getAttribute("memberUser");
        Set<Long> feedbackGiven = new HashSet<>();
        for (Event ev : completedEvents) {
            if (feedbackService.hasSubmitted(ev.getId(), member.getId())) {
                feedbackGiven.add(ev.getId());
            }
        }
        model.addAttribute("feedbackGiven", feedbackGiven);

        // Track which active + planned events this member has already registered for
        Set<Long> registeredEventIds = new HashSet<>();
        for (Event ev : activeEvents) {
            if (ticketService.isRegistered(ev.getId(), member.getId())) {
                registeredEventIds.add(ev.getId());
            }
        }
        for (Event ev : eventRepository.findByStatusOrderByEventDateAsc("Planned")) {
            if (ticketService.isRegistered(ev.getId(), member.getId())) {
                registeredEventIds.add(ev.getId());
            }
        }
        model.addAttribute("registeredEventIds", registeredEventIds);

        LocalDate today = LocalDate.now();
        model.addAttribute("activeActivities",
                activityRepository.findVisibleOnPortal(today));

        model.addAttribute("announcements", announcementService.findActiveForPortal());

        List<Member> activeMembers = memberRepository.findByApprovalStatusAndStatus("Approved", "Active");
        model.addAttribute("upcomingBirthdays", upcomingOccurrences(activeMembers, true, today, 30));
        model.addAttribute("upcomingAnniversaries", upcomingOccurrences(activeMembers, false, today, 30));

        return "portal/home";
    }

    private LinkedHashMap<Member, Integer> upcomingOccurrences(List<Member> members, boolean useDob,
                                                                LocalDate today, int windowDays) {
        return members.stream()
            .filter(m -> useDob ? m.getDateOfBirth() != null : m.getAnniversaryDate() != null)
            .map(m -> {
                LocalDate date = useDob ? m.getDateOfBirth() : m.getAnniversaryDate();
                int days = daysUntilNext(date.getMonthValue(), date.getDayOfMonth(), today);
                return new AbstractMap.SimpleEntry<>(m, days);
            })
            .filter(e -> e.getValue() <= windowDays)
            .sorted(Map.Entry.comparingByValue())
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                    (a, b) -> a, LinkedHashMap::new));
    }

    private int daysUntilNext(int month, int day, LocalDate today) {
        try {
            LocalDate next = LocalDate.of(today.getYear(), month, day);
            if (next.isBefore(today)) next = LocalDate.of(today.getYear() + 1, month, day);
            return (int) ChronoUnit.DAYS.between(today, next);
        } catch (Exception e) {
            LocalDate next = LocalDate.of(today.getYear(), month, 28);
            if (next.isBefore(today)) next = LocalDate.of(today.getYear() + 1, month, 28);
            return (int) ChronoUnit.DAYS.between(today, next);
        }
    }

    @PostMapping("/logout")
    public String logout(HttpSession session) {
        session.removeAttribute("memberLoggedIn");
        session.removeAttribute("memberUser");
        return "redirect:/member-login";
    }

    @GetMapping("/founders")
    public String founders() {
        return "portal/founders";
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
