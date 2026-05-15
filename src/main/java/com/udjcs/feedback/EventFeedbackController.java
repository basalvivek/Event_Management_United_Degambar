package com.udjcs.feedback;

import com.udjcs.member.Member;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/portal/feedback")
public class EventFeedbackController {

    private final EventFeedbackService service;

    public EventFeedbackController(EventFeedbackService service) {
        this.service = service;
    }

    @PostMapping
    public String submit(@RequestParam Long eventId,
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
                         HttpSession session,
                         RedirectAttributes attrs) {

        boolean hasContent = (overallExperience != null)
                || (satisfactionLevel != null)
                || (venueRating != null)
                || (enjoyedMost != null && !enjoyedMost.isBlank())
                || (timingsManaged != null)
                || (suggestions != null && !suggestions.isBlank())
                || (participateFuture != null)
                || (additionalComments != null && !additionalComments.isBlank());

        if (!hasContent) {
            attrs.addFlashAttribute("error", "Please fill in at least one field before submitting.");
            return "redirect:/portal";
        }

        Member member = (Member) session.getAttribute("memberUser");
        service.submit(eventId, member.getId(),
                overallExperience, satisfactionLevel,
                venueRating, programCoordinationRating, hospitalityRating, foodRating, spiritualRating,
                enjoyedMost, timingsManaged, suggestions, participateFuture, additionalComments);
        attrs.addFlashAttribute("success", "Thank you! Your feedback has been submitted.");
        return "redirect:/portal";
    }
}
