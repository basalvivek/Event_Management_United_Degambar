package com.udjcs.common;

import com.udjcs.activity.ActivityRepository;
import com.udjcs.assignment.AssignmentRepository;
import com.udjcs.event.EventRepository;
import com.udjcs.food.FoodRegistrationRepository;
import com.udjcs.hall.HallRegistrationRepository;
import com.udjcs.invitation.InvitationRegistrationRepository;
import com.udjcs.member.MemberRepository;
import com.udjcs.member.MemberService;
import com.udjcs.payment.PaymentRepository;
import com.udjcs.receivable.ReceivableTransactionRepository;
import com.udjcs.rehearsal.RehearsalRepository;
import com.udjcs.supportive.SupportiveOrganizationRepository;
import com.udjcs.ticket.EventTicketRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Controller
public class HomeController {

    private final MemberRepository memberRepository;
    private final MemberService memberService;
    private final EventRepository eventRepository;
    private final PaymentRepository paymentRepository;
    private final ActivityRepository activityRepository;
    private final RehearsalRepository rehearsalRepository;
    private final SupportiveOrganizationRepository supportiveOrganizationRepository;
    private final EventTicketRepository ticketRepository;
    private final HallRegistrationRepository hallRepository;
    private final FoodRegistrationRepository foodRepository;
    private final AssignmentRepository assignmentRepository;
    private final ReceivableTransactionRepository receivableRepository;
    private final InvitationRegistrationRepository invitationRepository;

    public HomeController(MemberRepository memberRepository,
                          MemberService memberService,
                          EventRepository eventRepository,
                          PaymentRepository paymentRepository,
                          ActivityRepository activityRepository,
                          RehearsalRepository rehearsalRepository,
                          SupportiveOrganizationRepository supportiveOrganizationRepository,
                          EventTicketRepository ticketRepository,
                          HallRegistrationRepository hallRepository,
                          FoodRegistrationRepository foodRepository,
                          AssignmentRepository assignmentRepository,
                          ReceivableTransactionRepository receivableRepository,
                          InvitationRegistrationRepository invitationRepository) {
        this.memberRepository = memberRepository;
        this.memberService = memberService;
        this.eventRepository = eventRepository;
        this.paymentRepository = paymentRepository;
        this.activityRepository = activityRepository;
        this.rehearsalRepository = rehearsalRepository;
        this.supportiveOrganizationRepository = supportiveOrganizationRepository;
        this.ticketRepository = ticketRepository;
        this.hallRepository = hallRepository;
        this.foodRepository = foodRepository;
        this.assignmentRepository = assignmentRepository;
        this.receivableRepository = receivableRepository;
        this.invitationRepository = invitationRepository;
    }

    @GetMapping("/")
    public String dashboard(Model model) {
        LocalDate today = LocalDate.now();

        // ── Members ──────────────────────────────────────────────────────────
        model.addAttribute("totalMembers",     memberRepository.count());
        model.addAttribute("activeMembers",    memberRepository.countByStatus("Active"));
        model.addAttribute("pendingApprovals", memberService.countPending());

        // ── Events ───────────────────────────────────────────────────────────
        model.addAttribute("totalEvents",        eventRepository.count());
        model.addAttribute("upcomingEventCount", eventRepository.countByEventDateGreaterThanEqual(today));

        // ── Activities & Assignments ─────────────────────────────────────────
        model.addAttribute("totalActivities",  activityRepository.count());
        model.addAttribute("activeActivities", activityRepository.countByStatus("Active"));
        model.addAttribute("totalAssignments", assignmentRepository.count());
        model.addAttribute("totalRehearsals",  rehearsalRepository.count());

        // ── Registration ──────────────────────────────────────────────────────
        model.addAttribute("hallBookings",       hallRepository.count());
        model.addAttribute("foodRegistrations",  foodRepository.count());
        model.addAttribute("totalInvitations",   invitationRepository.count());

        // ── Supporters ────────────────────────────────────────────────────────
        model.addAttribute("totalOrgs", supportiveOrganizationRepository.count());

        // ── Finance ───────────────────────────────────────────────────────────
        BigDecimal rawSum = paymentRepository.sumAllAmounts();
        model.addAttribute("totalFunds", rawSum != null ? rawSum : BigDecimal.ZERO);

        BigDecimal orgSum = paymentRepository.sumOrgDonations();
        model.addAttribute("orgDonationTotal", orgSum != null ? orgSum : BigDecimal.ZERO);
        model.addAttribute("orgDonationCount", paymentRepository.countOrgDonations());

        BigDecimal memberSum = paymentRepository.sumMemberDonations();
        model.addAttribute("memberDonationTotal", memberSum != null ? memberSum : BigDecimal.ZERO);
        model.addAttribute("memberDonationCount", paymentRepository.countMemberDonations());

        Long ticketSum = ticketRepository.sumAcceptedTickets();
        model.addAttribute("ticketTotal", ticketSum != null ? ticketSum : 0L);
        model.addAttribute("ticketCount", ticketRepository.countAcceptedTickets());

        BigDecimal receivableSum = receivableRepository.sumTotalAmount();
        model.addAttribute("totalReceivable", receivableSum != null ? receivableSum : BigDecimal.ZERO);
        model.addAttribute("receivableCount", receivableRepository.count());

        // ── Schedule data ─────────────────────────────────────────────────────
        model.addAttribute("upcomingEvents",
                eventRepository.findByStatusInOrderByEventDateAsc(List.of("Planned", "Active")));
        model.addAttribute("upcomingRehearsals",
                rehearsalRepository.findUpcomingWithActivity(today).stream().limit(5).toList());

        return "dashboard/index";
    }
}
