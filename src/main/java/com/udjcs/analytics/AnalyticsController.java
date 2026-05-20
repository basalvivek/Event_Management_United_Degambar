package com.udjcs.analytics;

import com.udjcs.activity.Activity;
import com.udjcs.activity.ActivityRepository;
import com.udjcs.assignment.Assignment;
import com.udjcs.assignment.AssignmentRepository;
import com.udjcs.event.Event;
import com.udjcs.event.EventRepository;
import com.udjcs.food.FoodRegistrationRepository;
import com.udjcs.hall.HallRegistrationRepository;
import com.udjcs.invitation.InvitationRegistrationRepository;
import com.udjcs.member.Member;
import com.udjcs.member.MemberRepository;
import com.udjcs.member.MemberService;
import com.udjcs.payable.PayableTransactionRepository;
import com.udjcs.payment.PaymentRepository;
import com.udjcs.receivable.ReceivableTransactionRepository;
import com.udjcs.rehearsal.Rehearsal;
import com.udjcs.rehearsal.RehearsalMemberRepository;
import com.udjcs.rehearsal.RehearsalRepository;
import com.udjcs.supportive.SupportiveOrganizationRepository;
import com.udjcs.ticket.EventTicket;
import com.udjcs.ticket.EventTicketRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/analytics")
public class AnalyticsController {

    private final MemberRepository        memberRepository;
    private final MemberService           memberService;
    private final EventRepository         eventRepository;
    private final PaymentRepository       paymentRepository;
    private final ActivityRepository      activityRepository;
    private final AssignmentRepository    assignmentRepository;
    private final RehearsalRepository     rehearsalRepository;
    private final RehearsalMemberRepository rehMemberRepository;
    private final SupportiveOrganizationRepository orgRepository;
    private final EventTicketRepository   ticketRepository;
    private final HallRegistrationRepository hallRepository;
    private final FoodRegistrationRepository foodRepository;
    private final InvitationRegistrationRepository invitationRepository;
    private final ReceivableTransactionRepository  receivableRepository;
    private final PayableTransactionRepository     payableRepository;

    public AnalyticsController(MemberRepository memberRepository,
                                MemberService memberService,
                                EventRepository eventRepository,
                                PaymentRepository paymentRepository,
                                ActivityRepository activityRepository,
                                AssignmentRepository assignmentRepository,
                                RehearsalRepository rehearsalRepository,
                                RehearsalMemberRepository rehMemberRepository,
                                SupportiveOrganizationRepository orgRepository,
                                EventTicketRepository ticketRepository,
                                HallRegistrationRepository hallRepository,
                                FoodRegistrationRepository foodRepository,
                                InvitationRegistrationRepository invitationRepository,
                                ReceivableTransactionRepository receivableRepository,
                                PayableTransactionRepository payableRepository) {
        this.memberRepository     = memberRepository;
        this.memberService        = memberService;
        this.eventRepository      = eventRepository;
        this.paymentRepository    = paymentRepository;
        this.activityRepository   = activityRepository;
        this.assignmentRepository = assignmentRepository;
        this.rehearsalRepository  = rehearsalRepository;
        this.rehMemberRepository  = rehMemberRepository;
        this.orgRepository        = orgRepository;
        this.ticketRepository     = ticketRepository;
        this.hallRepository       = hallRepository;
        this.foodRepository       = foodRepository;
        this.invitationRepository = invitationRepository;
        this.receivableRepository = receivableRepository;
        this.payableRepository    = payableRepository;
    }

    @GetMapping
    public String index(Model model) {
        LocalDate today = LocalDate.now();

        /* ── MEMBERS ── */
        List<Member> members = memberRepository.findAll();
        long totalMembers   = members.size();
        long activeMembers  = members.stream().filter(m -> "Active".equalsIgnoreCase(m.getStatus())).count();
        long pendingMembers = memberService.countPending();
        long inactiveMembers = totalMembers - activeMembers - pendingMembers;
        Map<String, Long> genderMap = members.stream()
            .collect(Collectors.groupingBy(m -> m.getGender() != null ? m.getGender() : "Unknown", Collectors.counting()));
        Map<String, Long> cityMap = members.stream()
            .filter(m -> m.getCity() != null && !m.getCity().isBlank())
            .collect(Collectors.groupingBy(Member::getCity, Collectors.counting()));
        List<Map.Entry<String, Long>> topCities = cityMap.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(6).collect(Collectors.toList());
        long maxCityCount = topCities.isEmpty() ? 1 : topCities.get(0).getValue();

        model.addAttribute("totalMembers",    totalMembers);
        model.addAttribute("activeMembers",   activeMembers);
        model.addAttribute("pendingMembers",  pendingMembers);
        model.addAttribute("inactiveMembers", Math.max(inactiveMembers, 0));
        model.addAttribute("genderMap",       genderMap);
        model.addAttribute("topCities",       topCities);
        model.addAttribute("maxCityCount",    maxCityCount);

        /* ── EVENTS ── */
        List<Event> events = eventRepository.findAll();
        long totalEvents    = events.size();
        long upcomingEvents = events.stream().filter(e -> e.getEventDate() != null && !e.getEventDate().isBefore(today)).count();
        long pastEvents     = totalEvents - upcomingEvents;
        Map<String, Long> eventStatusMap = events.stream()
            .collect(Collectors.groupingBy(e -> e.getStatus() != null ? e.getStatus() : "Unknown", Collectors.counting()));
        long maxEventStatus = eventStatusMap.values().stream().mapToLong(v -> v).max().orElse(1);

        model.addAttribute("totalEvents",    totalEvents);
        model.addAttribute("upcomingEvents", upcomingEvents);
        model.addAttribute("pastEvents",     pastEvents);
        model.addAttribute("eventStatusMap", eventStatusMap);
        model.addAttribute("maxEventStatus", maxEventStatus);

        /* ── TICKETS ── */
        List<EventTicket> tickets = ticketRepository.findAll();
        long totalTickets    = tickets.size();
        long acceptedTickets = tickets.stream().filter(t -> "Accepted".equalsIgnoreCase(t.getStatus())).count();
        long pendingTickets  = tickets.stream().filter(t -> "Pending".equalsIgnoreCase(t.getStatus())).count();
        long rejectedTickets = totalTickets - acceptedTickets - pendingTickets;
        Long ticketRevenue   = ticketRepository.sumAcceptedTickets();
        BigDecimal ticketRev = ticketRevenue != null ? BigDecimal.valueOf(ticketRevenue) : BigDecimal.ZERO;

        model.addAttribute("totalTickets",    totalTickets);
        model.addAttribute("acceptedTickets", acceptedTickets);
        model.addAttribute("pendingTickets",  pendingTickets);
        model.addAttribute("rejectedTickets", Math.max(rejectedTickets, 0));
        model.addAttribute("ticketRevenue",   ticketRev);

        /* ── FINANCE ── */
        BigDecimal totalDonations   = orZero(paymentRepository.sumAllAmounts());
        BigDecimal orgDonations     = orZero(paymentRepository.sumOrgDonations());
        BigDecimal memberDonations  = orZero(paymentRepository.sumMemberDonations());
        long       orgDonCount      = paymentRepository.countOrgDonations();
        long       mbrDonCount      = paymentRepository.countMemberDonations();
        BigDecimal totalReceivable  = orZero(receivableRepository.sumTotalAmount());
        BigDecimal totalPayable     = orZero(payableRepository.sumTotalAmount());
        BigDecimal netPosition      = totalReceivable.subtract(totalPayable);
        long       receivableCount  = receivableRepository.count();
        long       payableCount     = payableRepository.count();

        // For finance bar widths: proportion of total
        BigDecimal finMax = totalDonations.add(totalReceivable).add(ticketRev);
        int orgDonPct    = pct(orgDonations,    finMax);
        int mbrDonPct    = pct(memberDonations, finMax);
        int recPct       = pct(totalReceivable, finMax);
        int tikPct       = pct(ticketRev,       finMax);

        model.addAttribute("totalDonations",  totalDonations);
        model.addAttribute("orgDonations",    orgDonations);
        model.addAttribute("memberDonations", memberDonations);
        model.addAttribute("orgDonCount",     orgDonCount);
        model.addAttribute("mbrDonCount",     mbrDonCount);
        model.addAttribute("totalReceivable", totalReceivable);
        model.addAttribute("totalPayable",    totalPayable);
        model.addAttribute("netPosition",     netPosition);
        model.addAttribute("receivableCount", receivableCount);
        model.addAttribute("payableCount",    payableCount);
        model.addAttribute("ticketRevenue",   ticketRev);
        model.addAttribute("orgDonPct",       orgDonPct);
        model.addAttribute("mbrDonPct",       mbrDonPct);
        model.addAttribute("recPct",          recPct);
        model.addAttribute("tikPct",          tikPct);

        /* ── REGISTRATIONS ── */
        long hallCount       = hallRepository.count();
        long foodCount       = foodRepository.count();
        long invitationCount = invitationRepository.count();
        long supporterOrgs   = orgRepository.count();

        model.addAttribute("hallCount",       hallCount);
        model.addAttribute("foodCount",       foodCount);
        model.addAttribute("invitationCount", invitationCount);
        model.addAttribute("supporterOrgs",   supporterOrgs);

        /* ── ACTIVITIES ── */
        List<Activity> activities = activityRepository.findAllWithDetails();
        long totalActivities = activities.size();
        Map<String, Long> actStatusMap = activities.stream()
            .collect(Collectors.groupingBy(a -> a.getStatus() != null ? a.getStatus() : "Unknown", Collectors.counting()));
        long completedActivities  = actStatusMap.getOrDefault("Completed", 0L);
        long inProgressActivities = actStatusMap.getOrDefault("In Progress", 0L);
        long plannedActivities    = actStatusMap.getOrDefault("Planned", 0L);
        long totalAssignments     = assignmentRepository.count();

        // Role distribution
        List<Assignment> assignments = assignmentRepository.findAllWithDetails();
        Map<String, Long> roleMap = assignments.stream()
            .collect(Collectors.groupingBy(a -> a.getRole() != null ? a.getRole() : "Other", Collectors.counting()));
        List<Map.Entry<String, Long>> topRoles = roleMap.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(5).collect(Collectors.toList());
        long maxRoleCount = topRoles.isEmpty() ? 1 : topRoles.get(0).getValue();

        model.addAttribute("totalActivities",     totalActivities);
        model.addAttribute("actStatusMap",        actStatusMap);
        model.addAttribute("completedActivities", completedActivities);
        model.addAttribute("inProgressActivities",inProgressActivities);
        model.addAttribute("plannedActivities",   plannedActivities);
        model.addAttribute("totalAssignments",    totalAssignments);
        model.addAttribute("topRoles",            topRoles);
        model.addAttribute("maxRoleCount",        maxRoleCount);

        /* ── REHEARSALS ── */
        List<Rehearsal> rehearsals = rehearsalRepository.findAllWithDetails();
        long totalRehearsals     = rehearsals.size();
        Map<String, Long> rehStatusMap = rehearsals.stream()
            .collect(Collectors.groupingBy(r -> r.getStatus() != null ? r.getStatus() : "Unknown", Collectors.counting()));
        long completedRehearsals = rehStatusMap.getOrDefault("Completed", 0L);
        long scheduledRehearsals = rehStatusMap.getOrDefault("Scheduled", 0L);
        long cancelledRehearsals = rehStatusMap.getOrDefault("Cancelled", 0L);
        int  rehCompletionPct    = totalRehearsals > 0 ? (int)(completedRehearsals * 100 / totalRehearsals) : 0;

        // Per-activity rehearsal stats for the table
        Map<Long, Integer> actRehTotal = new HashMap<>();
        Map<Long, Integer> actRehDone  = new HashMap<>();
        for (Object[] row : rehearsalRepository.countAllByActivity())
            actRehTotal.put((Long) row[0], ((Number) row[1]).intValue());
        for (Object[] row : rehearsalRepository.countCompletedByActivity())
            actRehDone.put((Long) row[0], ((Number) row[1]).intValue());

        // Per-rehearsal attendance totals for per-activity attendance rates
        List<Long> rehIds = rehearsals.stream().map(Rehearsal::getId).collect(Collectors.toList());
        Map<Long, Long> rehTotalMbr  = new HashMap<>();  // rehearsalId -> total members
        Map<Long, Long> rehAttMbr    = new HashMap<>();  // rehearsalId -> attended
        if (!rehIds.isEmpty()) {
            for (Object[] row : rehMemberRepository.findAllMembersForRehearsalIds(rehIds)) {
                Long rid = (Long) row[0];
                rehTotalMbr.merge(rid, 1L, Long::sum);
                if (Boolean.TRUE.equals(row[4])) rehAttMbr.merge(rid, 1L, Long::sum);
            }
        }

        // Per-activity attendance rate
        List<Map<String, Object>> actAttRows = new ArrayList<>();
        for (Activity a : activities) {
            List<Rehearsal> actRehearsals = rehearsals.stream()
                .filter(r -> r.getActivity().getId().equals(a.getId()))
                .collect(Collectors.toList());
            if (actRehearsals.isEmpty()) continue;
            long totMbr = actRehearsals.stream().mapToLong(r -> rehTotalMbr.getOrDefault(r.getId(), 0L)).sum();
            long attMbr = actRehearsals.stream().mapToLong(r -> rehAttMbr.getOrDefault(r.getId(), 0L)).sum();
            int attPct  = totMbr > 0 ? (int)(attMbr * 100 / totMbr) : 0;
            int total   = actRehTotal.getOrDefault(a.getId(), 0);
            int done    = actRehDone.getOrDefault(a.getId(), 0);
            int compPct = total > 0 ? done * 100 / total : 0;
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("name",    a.getActivityName());
            row.put("status",  a.getStatus());
            row.put("total",   total);
            row.put("done",    done);
            row.put("compPct", compPct);
            row.put("totMbr",  totMbr);
            row.put("attMbr",  attMbr);
            row.put("attPct",  attPct);
            actAttRows.add(row);
        }
        actAttRows.sort((a, b) -> Integer.compare((int)b.get("compPct"), (int)a.get("compPct")));

        model.addAttribute("totalRehearsals",     totalRehearsals);
        model.addAttribute("rehStatusMap",        rehStatusMap);
        model.addAttribute("completedRehearsals", completedRehearsals);
        model.addAttribute("scheduledRehearsals", scheduledRehearsals);
        model.addAttribute("cancelledRehearsals", cancelledRehearsals);
        model.addAttribute("rehCompletionPct",    rehCompletionPct);
        model.addAttribute("actAttRows",          actAttRows);

        /* ── TOP-LEVEL KPIs ── */
        BigDecimal totalFunds = totalDonations.add(totalReceivable).add(ticketRev);
        int overallCompletionPct = totalActivities > 0
            ? (int)(completedActivities * 100 / totalActivities) : 0;

        model.addAttribute("totalFunds",            totalFunds);
        model.addAttribute("overallCompletionPct",  overallCompletionPct);

        return "analytics/index";
    }

    private static BigDecimal orZero(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    private static int pct(BigDecimal value, BigDecimal max) {
        if (max == null || max.compareTo(BigDecimal.ZERO) == 0) return 0;
        return value.multiply(BigDecimal.valueOf(100)).divide(max, 0, java.math.RoundingMode.HALF_UP).intValue();
    }
}
