package com.udjcs.aiagent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.udjcs.activity.Activity;
import com.udjcs.activity.ActivityRepository;
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
import com.udjcs.rehearsal.RehearsalRepository;
import com.udjcs.ticket.EventTicket;
import com.udjcs.ticket.EventTicketRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AIAgentService {

    @Value("${groq.api.key:}")
    private String apiKey;

    private final MemberRepository memberRepository;
    private final MemberService memberService;
    private final EventRepository eventRepository;
    private final ActivityRepository activityRepository;
    private final RehearsalRepository rehearsalRepository;
    private final AssignmentRepository assignmentRepository;
    private final PaymentRepository paymentRepository;
    private final ReceivableTransactionRepository receivableRepository;
    private final PayableTransactionRepository payableRepository;
    private final EventTicketRepository ticketRepository;
    private final HallRegistrationRepository hallRepository;
    private final FoodRegistrationRepository foodRepository;
    private final InvitationRegistrationRepository invitationRepository;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AIAgentService(MemberRepository memberRepository,
                          MemberService memberService,
                          EventRepository eventRepository,
                          ActivityRepository activityRepository,
                          RehearsalRepository rehearsalRepository,
                          AssignmentRepository assignmentRepository,
                          PaymentRepository paymentRepository,
                          ReceivableTransactionRepository receivableRepository,
                          PayableTransactionRepository payableRepository,
                          EventTicketRepository ticketRepository,
                          HallRegistrationRepository hallRepository,
                          FoodRegistrationRepository foodRepository,
                          InvitationRegistrationRepository invitationRepository) {
        this.memberRepository     = memberRepository;
        this.memberService        = memberService;
        this.eventRepository      = eventRepository;
        this.activityRepository   = activityRepository;
        this.rehearsalRepository  = rehearsalRepository;
        this.assignmentRepository = assignmentRepository;
        this.paymentRepository    = paymentRepository;
        this.receivableRepository = receivableRepository;
        this.payableRepository    = payableRepository;
        this.ticketRepository     = ticketRepository;
        this.hallRepository       = hallRepository;
        this.foodRepository       = foodRepository;
        this.invitationRepository = invitationRepository;
    }

    public String ask(String question) {
        if (apiKey == null || apiKey.isBlank()) {
            return "API key not configured. Please add 'groq.api.key=gsk_...' to application.properties and restart. Get a free key at console.groq.com";
        }
        try {
            return callClaude(question, buildContext());
        } catch (Exception e) {
            return "Sorry, I encountered an error: " + e.getMessage();
        }
    }

    private String buildContext() {
        StringBuilder sb = new StringBuilder();
        LocalDate today = LocalDate.now();
        sb.append("TODAY: ").append(today).append("\n\n");

        // Members
        List<Member> members = memberRepository.findAll();
        long active  = members.stream().filter(m -> "Active".equalsIgnoreCase(m.getStatus())).count();
        long pending = memberService.countPending();
        long inactive = Math.max(0, members.size() - active - pending);
        sb.append("=== MEMBERS (").append(members.size()).append(" total) ===\n");
        sb.append("Active: ").append(active).append(" | Pending Approval: ").append(pending)
          .append(" | Inactive: ").append(inactive).append("\n");
        for (Member m : members) {
            sb.append("  - ").append(m.getFirstName()).append(" ").append(m.getLastName())
              .append(" | Status: ").append(m.getStatus() != null ? m.getStatus() : "Unknown")
              .append(" | Gender: ").append(m.getGender() != null ? m.getGender() : "N/A")
              .append(" | City: ").append(m.getCity() != null ? m.getCity() : "N/A")
              .append("\n");
        }

        // Events
        List<Event> events = eventRepository.findAll();
        long upcoming = events.stream().filter(e -> e.getEventDate() != null && !e.getEventDate().isBefore(today)).count();
        sb.append("\n=== EVENTS (").append(events.size()).append(" total) ===\n");
        sb.append("Upcoming: ").append(upcoming).append(" | Past: ").append(events.size() - upcoming).append("\n");
        for (Event e : events) {
            sb.append("  - ").append(e.getEventName())
              .append(" | Date: ").append(e.getEventDate())
              .append(" | Type: ").append(e.getEventType() != null ? e.getEventType() : "N/A")
              .append(" | Status: ").append(e.getStatus() != null ? e.getStatus() : "N/A")
              .append("\n");
        }

        // Activities
        List<Activity> activities = activityRepository.findAllWithDetails();
        sb.append("\n=== ACTIVITIES (").append(activities.size()).append(" total) ===\n");
        for (Activity a : activities) {
            sb.append("  - ").append(a.getActivityName())
              .append(" | Status: ").append(a.getStatus())
              .append(" | From: ").append(a.getStartDate())
              .append(" | To: ").append(a.getEndDate())
              .append("\n");
        }

        // Rehearsals
        List<Rehearsal> rehearsals = rehearsalRepository.findAllWithDetails();
        sb.append("\n=== REHEARSALS (").append(rehearsals.size()).append(" total) ===\n");
        for (Rehearsal r : rehearsals) {
            String actName = r.getActivity() != null ? r.getActivity().getActivityName() : "N/A";
            sb.append("  - Activity: ").append(actName)
              .append(" | Date: ").append(r.getRehearsalDate())
              .append(" | Status: ").append(r.getStatus())
              .append(" | Venue: ").append(r.getVenue() != null ? r.getVenue() : "N/A")
              .append(" | Conductor: ").append(r.getConductedBy() != null ? r.getConductedBy() : "N/A")
              .append("\n");
        }

        // Finance
        BigDecimal totalDonations  = orZero(paymentRepository.sumAllAmounts());
        BigDecimal orgDonations    = orZero(paymentRepository.sumOrgDonations());
        BigDecimal mbrDonations    = orZero(paymentRepository.sumMemberDonations());
        BigDecimal totalReceivable = orZero(receivableRepository.sumTotalAmount());
        BigDecimal totalPayable    = orZero(payableRepository.sumTotalAmount());
        Long ticketRevL            = ticketRepository.sumAcceptedTickets();
        BigDecimal ticketRev       = ticketRevL != null ? BigDecimal.valueOf(ticketRevL) : BigDecimal.ZERO;
        sb.append("\n=== FINANCE ===\n");
        sb.append("Total Donations: £").append(totalDonations)
          .append(" (Org: £").append(orgDonations).append(", Member: £").append(mbrDonations).append(")\n");
        sb.append("Receivable: £").append(totalReceivable)
          .append(" | Payable: £").append(totalPayable)
          .append(" | Net Position: £").append(totalReceivable.subtract(totalPayable)).append("\n");
        sb.append("Ticket Revenue: £").append(ticketRev).append("\n");

        // Tickets
        List<EventTicket> tickets = ticketRepository.findAll();
        long accepted = tickets.stream().filter(t -> "Accepted".equalsIgnoreCase(t.getStatus())).count();
        long pend     = tickets.stream().filter(t -> "Pending".equalsIgnoreCase(t.getStatus())).count();
        sb.append("\n=== TICKETS (").append(tickets.size()).append(" total) ===\n");
        sb.append("Accepted: ").append(accepted).append(" | Pending: ").append(pend)
          .append(" | Rejected: ").append(Math.max(0, tickets.size() - accepted - pend)).append("\n");

        // Founders
        sb.append("\n=== FOUNDERS (5 founding members) ===\n");
        sb.append("  1. Vivek Basal       | Role: Founder — Technical     | Led the digital platform, software architecture & technology strategy\n");
        sb.append("  2. Shreyans Jain     | Role: Founder — Marketing     | Community outreach, brand building & growing membership presence\n");
        sb.append("  3. Jignesh Shah      | Role: Founder — Coordination  | Operations, event coordination & inter-organisational relationships\n");
        sb.append("  4. Rakesh Jain       | Role: Founder — Logistics     | End-to-end event execution, venue arrangements & resource allocation\n");
        sb.append("  5. Ashish Jain       | Role: Founder — Finance       | Financial stewardship, accounting & sustainable funding management\n");

        // Registrations
        sb.append("\n=== REGISTRATIONS ===\n");
        sb.append("Hall Bookings: ").append(hallRepository.count())
          .append(" | Food Registrations: ").append(foodRepository.count())
          .append(" | Invitations: ").append(invitationRepository.count())
          .append(" | Assignments: ").append(assignmentRepository.count())
          .append("\n");

        return sb.toString();
    }

    private String callClaude(String question, String context) throws Exception {
        String systemPrompt = "You are an AI assistant for the United Digambar Jain Community System (UDJCS), " +
                "a charitable trust management system. Answer questions concisely and clearly using the live " +
                "data provided. Be friendly and professional. If data is unavailable, say so politely.";

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", "llama-3.3-70b-versatile");
        body.put("max_tokens", 1024);
        body.put("messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user",   "content",
                       "Current UDJCS system data:\n\n" + context + "\n\nQuestion: " + question)));

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("https://api.groq.com/openai/v1/chat/completions"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                .timeout(Duration.ofSeconds(30))
                .build();

        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        return parseResponse(resp.body());
    }

    @SuppressWarnings("unchecked")
    private String parseResponse(String body) throws Exception {
        Map<String, Object> resp = objectMapper.readValue(body, Map.class);
        if (resp.containsKey("error")) {
            Map<String, Object> err = (Map<String, Object>) resp.get("error");
            return "Error: " + err.getOrDefault("message", "Unknown error");
        }
        List<Map<String, Object>> choices = (List<Map<String, Object>>) resp.get("choices");
        if (choices == null || choices.isEmpty()) return "No response received.";
        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        return (String) message.get("content");
    }

    private BigDecimal orZero(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
