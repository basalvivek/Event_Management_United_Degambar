package com.udjcs.member;

import com.udjcs.activity.ActivityRepository;
import com.udjcs.assignment.Assignment;
import com.udjcs.assignment.AssignmentRepository;
import com.udjcs.event.EventRepository;
import com.udjcs.feedback.EventFeedbackService;
import com.udjcs.gallery.GalleryService;
import com.udjcs.ticket.EventTicket;
import com.udjcs.ticket.EventTicketRepository;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import org.springframework.validation.FieldError;

@Controller
@RequestMapping("/members")
public class MemberController {

    private final MemberService service;
    private final MemberRepository memberRepository;
    private final EventRepository eventRepository;
    private final EventTicketRepository ticketRepository;
    private final EventFeedbackService feedbackService;
    private final AssignmentRepository assignmentRepository;
    private final ActivityRepository activityRepository;
    private final GalleryService galleryService;

    public MemberController(MemberService service, MemberRepository memberRepository,
                             EventRepository eventRepository, EventTicketRepository ticketRepository,
                             EventFeedbackService feedbackService,
                             AssignmentRepository assignmentRepository,
                             ActivityRepository activityRepository,
                             GalleryService galleryService) {
        this.service = service;
        this.memberRepository = memberRepository;
        this.eventRepository = eventRepository;
        this.ticketRepository = ticketRepository;
        this.feedbackService = feedbackService;
        this.assignmentRepository = assignmentRepository;
        this.activityRepository = activityRepository;
        this.galleryService = galleryService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("items", service.findAll());
        model.addAttribute("galleryImageIds", galleryService.findAllIds());
        return "member/list";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        Member m = new Member();
        m.setStatus("Active");
        m.setMembershipType("General");
        m.setApprovalStatus("Pending");
        m.setMembershipDate(LocalDate.now());
        model.addAttribute("item", m);
        return "member/form";
    }

    @PostMapping
    public String create(@ModelAttribute("item") @Valid Member member,
                         BindingResult result, Model model,
                         RedirectAttributes attrs) {
        if (member.getEmail() != null && !member.getEmail().isBlank()
                && memberRepository.existsByEmailIgnoreCase(member.getEmail())) {
            result.addError(new FieldError("item", "email",
                    "This email address is already registered to another member."));
        }
        if (result.hasErrors()) {
            model.addAttribute("item", member);
            return "member/form";
        }
        service.save(member);
        attrs.addFlashAttribute("success", "Member registered successfully.");
        return "redirect:/members";
    }

    @GetMapping("/{id}")
    public String redirectToEdit(@PathVariable Long id) {
        return "redirect:/members/" + id + "/edit";
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model) {
        model.addAttribute("item", service.findById(id));
        List<com.udjcs.event.Event> availableEvents = eventRepository
                .findByStatusInOrderByEventDateAsc(java.util.List.of("Planned", "Active"));
        List<com.udjcs.event.Event> completedEvents = eventRepository
                .findByStatusInOrderByEventDateAsc(java.util.List.of("Completed"));
        List<EventTicket> memberTickets = ticketRepository.findByMemberIdWithEvent(id);
        java.util.Set<Long> registeredEventIds = memberTickets.stream()
                .map(t -> t.getEvent().getId()).collect(java.util.stream.Collectors.toSet());
        java.util.Set<Long> feedbackGivenEventIds = completedEvents.stream()
                .filter(e -> feedbackService.hasSubmitted(e.getId(), id))
                .map(com.udjcs.event.Event::getId)
                .collect(java.util.stream.Collectors.toSet());
        model.addAttribute("availableEvents", availableEvents);
        model.addAttribute("completedEvents", completedEvents);
        model.addAttribute("memberTickets", memberTickets);
        model.addAttribute("registeredEventIds", registeredEventIds);
        model.addAttribute("feedbackGivenEventIds", feedbackGivenEventIds);
        List<Assignment> memberAssignments = assignmentRepository.findByMemberIdWithDetails(id);
        java.util.Set<Long> assignedActivityIds = memberAssignments.stream()
                .map(a -> a.getActivity().getId()).collect(java.util.stream.Collectors.toSet());
        model.addAttribute("memberAssignments", memberAssignments);
        model.addAttribute("availableActivities", activityRepository.findByStatusInWithDetails(
                java.util.List.of("Planned", "In Progress")));
        model.addAttribute("assignedActivityIds", assignedActivityIds);
        return "member/form";
    }

    @PostMapping("/{id}/register-event")
    public String registerForEvent(@PathVariable Long id,
                                    @RequestParam Long eventId,
                                    @RequestParam(defaultValue = "1") Integer adultCount,
                                    @RequestParam(defaultValue = "0") Integer youngerCount,
                                    @RequestParam(defaultValue = "0") Integer childCount,
                                    RedirectAttributes attrs) {
        if (ticketRepository.existsByEvent_IdAndMember_Id(eventId, id)) {
            attrs.addFlashAttribute("error", "Member is already registered for this event.");
            return "redirect:/members/" + id + "/edit";
        }
        com.udjcs.event.Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found: " + eventId));
        com.udjcs.member.Member member = service.findById(id);

        int adultPrice   = event.getTicketAdult()   != null ? event.getTicketAdult()   : 0;
        int youngerPrice = event.getTicketYounger()  != null ? event.getTicketYounger() : 0;
        int childPrice   = event.getTicketChild()    != null ? event.getTicketChild()   : 0;

        EventTicket ticket = new EventTicket();
        ticket.setEvent(event);
        ticket.setMember(member);
        ticket.setAdultCount(adultCount);
        ticket.setYoungerCount(youngerCount);
        ticket.setChildCount(childCount);
        ticket.setAdultAmount(adultCount * adultPrice);
        ticket.setYoungerAmount(youngerCount * youngerPrice);
        ticket.setChildAmount(childCount * childPrice);
        ticket.setTotalAmount(adultCount * adultPrice + youngerCount * youngerPrice + childCount * childPrice);
        ticket.setStatus("Pending");
        ticketRepository.save(ticket);

        attrs.addFlashAttribute("success", "Registered for " + event.getEventName() + ". Status: Pending approval.");
        return "redirect:/members/" + id + "/edit";
    }

    @PostMapping("/{id}/feedback")
    public String submitFeedback(@PathVariable Long id,
                                  @RequestParam Long eventId,
                                  @RequestParam(required = false) String overallExperience,
                                  @RequestParam(required = false) String satisfactionLevel,
                                  @RequestParam(required = false) Integer venueRating,
                                  @RequestParam(required = false) Integer programCoordinationRating,
                                  @RequestParam(required = false) Integer hospitalityRating,
                                  @RequestParam(required = false) Integer foodRating,
                                  @RequestParam(required = false) Integer spiritualRating,
                                  @RequestParam(required = false) String enjoyedMost,
                                  @RequestParam(required = false) String timingsManaged,
                                  @RequestParam(required = false) String suggestions,
                                  @RequestParam(required = false) String participateFuture,
                                  @RequestParam(required = false) String additionalComments,
                                  RedirectAttributes attrs) {
        if (feedbackService.hasSubmitted(eventId, id)) {
            attrs.addFlashAttribute("error", "Feedback already submitted for this event.");
            return "redirect:/members/" + id + "/edit";
        }
        feedbackService.submit(eventId, id,
                overallExperience, satisfactionLevel,
                venueRating, programCoordinationRating, hospitalityRating, foodRating, spiritualRating,
                enjoyedMost, timingsManaged, suggestions, participateFuture, additionalComments);
        attrs.addFlashAttribute("success", "Feedback submitted successfully.");
        return "redirect:/members/" + id + "/edit";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @ModelAttribute("item") @Valid Member member,
                         BindingResult result, Model model,
                         RedirectAttributes attrs) {
        if (member.getEmail() != null && !member.getEmail().isBlank()
                && memberRepository.existsByEmailIgnoreCaseAndIdNot(member.getEmail(), id)) {
            result.addError(new FieldError("item", "email",
                    "This email address is already registered to another member."));
        }
        if (result.hasErrors()) {
            model.addAttribute("item", member);
            return "member/form";
        }
        member.setId(id);
        service.save(member);
        attrs.addFlashAttribute("success", "Member updated successfully.");
        return "redirect:/members";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes attrs) {
        service.deleteById(id);
        attrs.addFlashAttribute("success", "Member deleted successfully.");
        return "redirect:/members";
    }

    @GetMapping("/pending")
    public String pendingList(Model model) {
        model.addAttribute("items", service.findPending());
        return "member/pending";
    }

    @PostMapping("/{id}/approve")
    public String approve(@PathVariable Long id, RedirectAttributes attrs) {
        service.approve(id);
        attrs.addFlashAttribute("success", "Member approved successfully.");
        return "redirect:/members/pending";
    }

    @GetMapping("/{id}/photo")
    @ResponseBody
    public ResponseEntity<byte[]> servePhoto(@PathVariable Long id) {
        Member member = service.findById(id);
        if (member.getProfilePicture() == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(member.getPhotoMimeType()))
                .body(member.getProfilePicture());
    }

    @PostMapping("/{id}/photo")
    public String uploadPhoto(@PathVariable Long id,
                              @RequestParam("photoFile") MultipartFile file,
                              RedirectAttributes attrs) throws IOException {
        if (file == null || file.isEmpty()) {
            attrs.addFlashAttribute("error", "Please select an image to upload.");
            return "redirect:/members/" + id + "/edit";
        }
        if (file.getSize() > 2_097_152) {
            attrs.addFlashAttribute("error", "Photo must be 2 MB or smaller.");
            return "redirect:/members/" + id + "/edit";
        }
        String ct = file.getContentType();
        if (ct == null || !ct.startsWith("image/")) {
            attrs.addFlashAttribute("error", "File must be an image.");
            return "redirect:/members/" + id + "/edit";
        }
        service.savePhoto(id, file);
        attrs.addFlashAttribute("success", "Profile photo updated.");
        return "redirect:/members/" + id + "/edit";
    }

    @PostMapping("/{id}/photo/delete")
    public String deletePhoto(@PathVariable Long id, RedirectAttributes attrs) {
        service.deletePhoto(id);
        attrs.addFlashAttribute("success", "Profile photo removed.");
        return "redirect:/members/" + id + "/edit";
    }

    @GetMapping("/{id}/qr")
    @ResponseBody
    public ResponseEntity<byte[]> serveQr(@PathVariable Long id) {
        Member member = service.findById(id);
        if (member.getQrCode() == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .header("Content-Disposition",
                        "attachment; filename=\"member-" + id + "-qr.png\"")
                .body(member.getQrCode());
    }
}
