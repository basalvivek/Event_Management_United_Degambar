package com.udjcs.common;

import com.udjcs.activity.ActivityRepository;
import com.udjcs.event.EventRepository;
import com.udjcs.member.MemberRepository;
import com.udjcs.payment.PaymentRepository;
import com.udjcs.rehearsal.RehearsalRepository;
import com.udjcs.supportive.SupportiveOrganizationRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.time.LocalDate;

@Controller
public class HomeController {

    private final MemberRepository memberRepository;
    private final EventRepository eventRepository;
    private final PaymentRepository paymentRepository;
    private final ActivityRepository activityRepository;
    private final RehearsalRepository rehearsalRepository;
    private final SupportiveOrganizationRepository supportiveOrganizationRepository;

    public HomeController(MemberRepository memberRepository,
                          EventRepository eventRepository,
                          PaymentRepository paymentRepository,
                          ActivityRepository activityRepository,
                          RehearsalRepository rehearsalRepository,
                          SupportiveOrganizationRepository supportiveOrganizationRepository) {
        this.memberRepository = memberRepository;
        this.eventRepository = eventRepository;
        this.paymentRepository = paymentRepository;
        this.activityRepository = activityRepository;
        this.rehearsalRepository = rehearsalRepository;
        this.supportiveOrganizationRepository = supportiveOrganizationRepository;
    }

    @GetMapping("/")
    public String dashboard(Model model) {
        LocalDate today = LocalDate.now();

        model.addAttribute("totalMembers", memberRepository.count());
        model.addAttribute("activeMembers", memberRepository.countByStatus("Active"));
        model.addAttribute("totalEvents", eventRepository.count());
        model.addAttribute("upcomingEventCount", eventRepository.countByEventDateGreaterThanEqual(today));
        model.addAttribute("totalActivities", activityRepository.count());
        model.addAttribute("activeActivities", activityRepository.countByStatus("Active"));
        model.addAttribute("totalOrgs", supportiveOrganizationRepository.count());

        BigDecimal rawSum = paymentRepository.sumAllAmounts();
        model.addAttribute("totalFunds", rawSum != null ? rawSum : BigDecimal.ZERO);

        model.addAttribute("upcomingEvents",
                eventRepository.findTop5ByEventDateGreaterThanEqualOrderByEventDateAsc(today));
        model.addAttribute("upcomingRehearsals",
                rehearsalRepository.findUpcomingWithActivity(today).stream().limit(5).toList());

        return "dashboard/index";
    }
}
