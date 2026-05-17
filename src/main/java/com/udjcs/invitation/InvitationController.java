package com.udjcs.invitation;

import com.udjcs.common.GlobalControllerAdvice;
import com.udjcs.event.EventRepository;
import com.udjcs.eventprogram.EventProgramRepository;
import com.udjcs.organization.OrganizationRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/invitations")
public class InvitationController {

    private final InvitationService service;
    private final InvitationRegistrationService registrationService;
    private final OrganizationRepository organizationRepository;
    private final EventRepository eventRepository;
    private final EventProgramRepository eventProgramRepository;
    private final GlobalControllerAdvice globalAdvice;

    public InvitationController(InvitationService service,
                                  InvitationRegistrationService registrationService,
                                  OrganizationRepository organizationRepository,
                                  EventRepository eventRepository,
                                  EventProgramRepository eventProgramRepository,
                                  GlobalControllerAdvice globalAdvice) {
        this.service = service;
        this.registrationService = registrationService;
        this.organizationRepository = organizationRepository;
        this.eventRepository = eventRepository;
        this.eventProgramRepository = eventProgramRepository;
        this.globalAdvice = globalAdvice;
    }

    @GetMapping("/{id}/registrations")
    public String viewRegistrations(@PathVariable Long id, Model model) {
        Invitation inv = service.findById(id);
        model.addAttribute("invitation", inv);
        model.addAttribute("registrations", registrationService.findByInvitationId(id));
        return "invitation/registrations";
    }

    @GetMapping("/event-programs/{eventId}")
    @ResponseBody
    public List<Map<String, String>> getEventPrograms(@PathVariable Long eventId) {
        return eventProgramRepository.findByEventIdWithDetails(eventId).stream()
                .map(ep -> {
                    Map<String, String> m = new LinkedHashMap<>();
                    m.put("name",  ep.getProgramName());
                    m.put("start", ep.getStartTime() != null ? ep.getStartTime().toString() : "");
                    m.put("end",   ep.getEndTime()   != null ? ep.getEndTime().toString()   : "");
                    m.put("desc",  ep.getProgramDescription() != null ? ep.getProgramDescription() : "");
                    return m;
                })
                .collect(Collectors.toList());
    }

    @GetMapping
    public String list(Model model) {
        List<Invitation> items = service.findAll();
        java.util.Map<Long, Long> counts = new java.util.LinkedHashMap<>();
        java.util.Map<Long, java.util.List<InvitationRegistration>> regMap = new java.util.LinkedHashMap<>();
        for (Invitation inv : items) {
            counts.put(inv.getId(), registrationService.countByInvitationId(inv.getId()));
            regMap.put(inv.getId(), registrationService.findByInvitationId(inv.getId()));
        }
        model.addAttribute("items", items);
        model.addAttribute("counts", counts);
        model.addAttribute("regMap", regMap);
        return "invitation/list";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("item", new Invitation());
        model.addAttribute("org", organizationRepository.findAll().stream().findFirst().orElse(null));
        model.addAttribute("events", eventRepository.findAll(Sort.by(Sort.Direction.ASC, "eventName")));
        return "invitation/form";
    }

    @PostMapping
    public String create(@ModelAttribute("item") @Valid Invitation invitation,
                         BindingResult result,
                         @RequestParam(value = "bannerFile", required = false) MultipartFile bannerFile,
                         @RequestParam(value = "activityName", required = false) List<String> activityNames,
                         @RequestParam(value = "startTime", required = false) List<String> startTimes,
                         @RequestParam(value = "endTime", required = false) List<String> endTimes,
                         @RequestParam(value = "activityDesc", required = false) List<String> activityDescs,
                         Model model,
                         HttpServletRequest request,
                         RedirectAttributes attrs) throws IOException {
        if (result.hasErrors()) {
            model.addAttribute("org", organizationRepository.findAll().stream().findFirst().orElse(null));
            model.addAttribute("events", eventRepository.findAll(Sort.by(Sort.Direction.ASC, "eventName")));
            return "invitation/form";
        }
        service.save(invitation, bannerFile, activityNames, startTimes, endTimes, activityDescs);
        String base = globalAdvice.resolveBaseUrl(request);
        attrs.addFlashAttribute("success", "Invitation created! Public URL: " + base + "/e/" + invitation.getSlug());
        return "redirect:/invitations";
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model) {
        model.addAttribute("item", service.findByIdForDisplay(id));
        model.addAttribute("org", organizationRepository.findAll().stream().findFirst().orElse(null));
        model.addAttribute("events", eventRepository.findAll(Sort.by(Sort.Direction.ASC, "eventName")));
        return "invitation/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @ModelAttribute("item") @Valid Invitation invitation,
                         BindingResult result,
                         @RequestParam(value = "bannerFile", required = false) MultipartFile bannerFile,
                         @RequestParam(value = "activityName", required = false) List<String> activityNames,
                         @RequestParam(value = "startTime", required = false) List<String> startTimes,
                         @RequestParam(value = "endTime", required = false) List<String> endTimes,
                         @RequestParam(value = "activityDesc", required = false) List<String> activityDescs,
                         Model model,
                         RedirectAttributes attrs) throws IOException {
        if (result.hasErrors()) {
            model.addAttribute("org", organizationRepository.findAll().stream().findFirst().orElse(null));
            model.addAttribute("events", eventRepository.findAll(Sort.by(Sort.Direction.ASC, "eventName")));
            return "invitation/form";
        }
        invitation.setId(id);
        service.save(invitation, bannerFile, activityNames, startTimes, endTimes, activityDescs);
        attrs.addFlashAttribute("success", "Invitation updated.");
        return "redirect:/invitations";
    }

    @PostMapping("/{id}/banner/delete")
    public String deleteBanner(@PathVariable Long id, RedirectAttributes attrs) {
        service.deleteBanner(id);
        attrs.addFlashAttribute("success", "Banner removed.");
        return "redirect:/invitations/" + id + "/edit";
    }

    @GetMapping("/{id}/banner")
    @ResponseBody
    public ResponseEntity<byte[]> serveBanner(@PathVariable Long id) {
        Invitation inv = service.findById(id);
        if (inv.getBannerImage() == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(inv.getBannerMimeType()))
                .body(inv.getBannerImage());
    }

    @GetMapping("/{id}/export")
    @ResponseBody
    public ResponseEntity<byte[]> exportHtml(@PathVariable Long id) {
        Invitation inv = service.findByIdForDisplay(id);

        String orgLogoTag = "<div class=\"org-logo-placeholder\"><svg xmlns='http://www.w3.org/2000/svg' width='28' height='28' fill='rgba(255,255,255,0.7)' viewBox='0 0 16 16'><path d='M8.277.084a.5.5 0 0 0-.554 0l-7.5 5A.5.5 0 0 0 .5 6H2v.5a.5.5 0 0 0 .5.5h11a.5.5 0 0 0 .5-.5V6h1.5a.5.5 0 0 0 .277-.916l-7.5-5z'/><path d='M3 6.5v5a.5.5 0 0 0 .5.5h9a.5.5 0 0 0 .5-.5v-5h-10z'/></svg></div>";
        if (inv.getOrganization() != null && inv.getOrganization().getLogoData() != null) {
            String b64 = Base64.getEncoder().encodeToString(inv.getOrganization().getLogoData());
            String mime = inv.getOrganization().getLogoMimeType() != null ? inv.getOrganization().getLogoMimeType() : "image/jpeg";
            orgLogoTag = "<img class=\"org-logo\" src=\"data:" + mime + ";base64," + b64 + "\" alt=\"Logo\">";
        }

        String heroBgStyle = "";
        if (inv.getBannerImage() != null) {
            String b64 = Base64.getEncoder().encodeToString(inv.getBannerImage());
            String mime = inv.getBannerMimeType() != null ? inv.getBannerMimeType() : "image/jpeg";
            heroBgStyle = "<div class=\"hero-bg\" style=\"background-image:url('data:" + mime + ";base64," + b64 + "')\"></div>";
        }

        String orgName = inv.getOrganization() != null ? esc(inv.getOrganization().getName()) : "United Digambar Jain Community";
        String orgType = inv.getOrganization() != null && inv.getOrganization().getTrustType() != null ? esc(inv.getOrganization().getTrustType()) : "";
        String dateStr = inv.getEventDate() != null ? inv.getEventDate().format(DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy")) : "";
        String fullAddress = (inv.getAddress() != null ? esc(inv.getAddress()) : "") +
                (inv.getPostcode() != null && !inv.getPostcode().isBlank() ? ", " + esc(inv.getPostcode()) : "");

        StringBuilder activities = new StringBuilder();
        for (InvitationActivity a : inv.getActivities()) {
            activities.append("<div class=\"schedule-row\">")
                .append("<div class=\"schedule-time\">")
                .append(a.getStartTime() != null ? "<span>" + esc(a.getStartTime()) + "</span>" : "")
                .append(a.getEndTime() != null ? "<span class=\"time-range\">until " + esc(a.getEndTime()) + "</span>" : "")
                .append("</div><div>")
                .append("<div class=\"schedule-name\">").append(esc(a.getActivityName())).append("</div>")
                .append(a.getDescription() != null && !a.getDescription().isBlank()
                        ? "<div class=\"schedule-desc\">" + esc(a.getDescription()) + "</div>" : "")
                .append("</div></div>");
        }

        String html = buildExportHtml(orgLogoTag, heroBgStyle, orgName, orgType,
                inv.getEventTag() != null ? esc(inv.getEventTag()) : null,
                esc(inv.getEventTitle()), dateStr, fullAddress,
                inv.getAddress() != null ? esc(inv.getAddress()) : null,
                inv.getPostcode() != null ? esc(inv.getPostcode()) : null,
                activities.toString(),
                inv.getAboutEvent() != null ? esc(inv.getAboutEvent()) : null);

        byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
        String filename = inv.getSlug() + ".html";
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                .contentType(new MediaType("text", "html", StandardCharsets.UTF_8))
                .body(bytes);
    }

    private String buildExportHtml(String orgLogoTag, String heroBgStyle, String orgName,
            String orgType, String tag, String title, String dateStr, String fullAddress,
            String address, String postcode, String activitiesHtml, String about) {
        return "<!DOCTYPE html><html><head><meta charset='UTF-8'>" +
            "<meta name='viewport' content='width=device-width,initial-scale=1'>" +
            "<title>" + title + " — Invitation</title>" +
            "<link rel='stylesheet' href='https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css'>" +
            "<link rel='stylesheet' href='https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.css'>" +
            "<style>*{box-sizing:border-box;margin:0;padding:0}body{font-family:'Segoe UI',system-ui,sans-serif;background:#f8f7ff;color:#1e1b4b;padding-bottom:70px}" +
            ".hero{position:relative;min-height:320px;background:linear-gradient(135deg,#4f46e5 0%,#7c3aed 60%,#a855f7 100%);display:flex;flex-direction:column;align-items:center;justify-content:flex-end;overflow:hidden}" +
            ".hero-bg{position:absolute;inset:0;background-size:cover;background-position:center;filter:brightness(0.45)}" +
            ".hero-overlay{position:absolute;inset:0;background:linear-gradient(to bottom,rgba(30,27,75,0.25) 0%,rgba(30,27,75,0.75) 100%)}" +
            ".hero-content{position:relative;z-index:2;width:100%;max-width:800px;padding:36px 24px}" +
            ".org-row{display:flex;align-items:center;gap:12px;margin-bottom:18px}" +
            ".org-logo{width:50px;height:50px;border-radius:10px;object-fit:contain;background:rgba(255,255,255,0.12);padding:4px;border:1px solid rgba(255,255,255,0.3)}" +
            ".org-logo-placeholder{width:50px;height:50px;border-radius:10px;background:rgba(255,255,255,0.15);border:1px solid rgba(255,255,255,0.25);display:flex;align-items:center;justify-content:center;flex-shrink:0}" +
            ".org-name{color:rgba(255,255,255,0.9);font-size:0.82rem;font-weight:600}" +
            ".org-sub{color:rgba(255,255,255,0.55);font-size:0.7rem}" +
            ".event-tag{display:inline-block;background:rgba(255,255,255,0.2);color:rgba(255,255,255,0.9);border-radius:20px;padding:3px 14px;font-size:0.72rem;font-weight:700;letter-spacing:1px;text-transform:uppercase;border:1px solid rgba(255,255,255,0.25);margin-bottom:10px}" +
            ".event-title{color:#fff;font-size:1.9rem;font-weight:800;line-height:1.2;margin-bottom:14px;text-shadow:0 2px 8px rgba(0,0,0,0.3)}" +
            ".event-meta{display:flex;flex-wrap:wrap;gap:14px}" +
            ".meta-chip{display:flex;align-items:center;gap:7px;color:rgba(255,255,255,0.85);font-size:0.83rem}" +
            ".page-body{max-width:800px;margin:0 auto;padding:28px 20px 40px}" +
            ".section-card{background:#fff;border-radius:14px;padding:22px 26px;box-shadow:0 2px 16px rgba(79,70,229,0.08);margin-bottom:18px}" +
            ".section-title{font-size:0.67rem;font-weight:800;letter-spacing:1.4px;text-transform:uppercase;color:#6366f1;margin-bottom:14px;display:flex;align-items:center;gap:8px}" +
            ".section-title::after{content:'';flex:1;height:1px;background:#e0e7ff}" +
            ".schedule-row{display:grid;grid-template-columns:110px 1fr;gap:0 14px;padding:10px 0;border-bottom:1px solid #f1f0ff;align-items:start}" +
            ".schedule-row:last-child{border-bottom:none;padding-bottom:0}" +
            ".schedule-time{font-size:0.78rem;font-weight:700;color:#6366f1;white-space:nowrap}" +
            ".time-range{color:#a5b4fc;font-weight:500;font-size:0.72rem;display:block}" +
            ".schedule-name{font-weight:600;font-size:0.9rem;color:#1e1b4b}" +
            ".schedule-desc{color:#64748b;font-size:0.8rem;margin-top:2px}" +
            ".about-text{color:#475569;font-size:0.9rem;line-height:1.75;white-space:pre-wrap}" +
            ".inv-footer{text-align:center;padding:20px 0 8px;color:#94a3b8;font-size:0.73rem}" +
            ".inv-footer strong{color:#6366f1}" +
            ".share-bar{position:fixed;bottom:0;left:0;right:0;background:rgba(255,255,255,0.96);backdrop-filter:blur(10px);border-top:1px solid #e0e7ff;padding:10px 20px;display:flex;align-items:center;justify-content:center;gap:10px;box-shadow:0 -4px 20px rgba(79,70,229,0.1)}" +
            ".share-url{font-family:monospace;font-size:0.78rem;color:#4f46e5;background:#eef2ff;border:1px solid #c7d2fe;border-radius:6px;padding:5px 12px;max-width:300px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}" +
            ".btn-copy{background:#4f46e5;color:#fff;border:none;border-radius:8px;padding:7px 16px;font-size:0.82rem;font-weight:600;cursor:pointer;white-space:nowrap;display:flex;align-items:center;gap:6px;transition:background 0.15s}" +
            ".btn-copy:hover{background:#4338ca}.btn-copy.copied{background:#059669}" +
            "@media(max-width:600px){.event-title{font-size:1.4rem}.hero-content{padding:24px 16px}.section-card{padding:16px}.share-url{max-width:160px}}</style>" +
            "</head><body>" +
            "<div class='hero'>" + heroBgStyle +
            "<div class='hero-overlay'></div>" +
            "<div class='hero-content'>" +
            "<div class='org-row'>" + orgLogoTag +
            "<div><div class='org-name'>" + orgName + "</div><div class='org-sub'>" + orgType + "</div></div></div>" +
            (tag != null ? "<div class='event-tag'>" + tag + "</div>" : "") +
            "<div class='event-title'>" + title + "</div>" +
            "<div class='event-meta'>" +
            (!dateStr.isBlank() ? "<div class='meta-chip'><i class='bi bi-calendar3'></i><span>" + dateStr + "</span></div>" : "") +
            (!fullAddress.isBlank() ? "<div class='meta-chip'><i class='bi bi-geo-alt'></i><span>" + fullAddress + "</span></div>" : "") +
            "</div></div></div>" +
            "<div class='page-body'>" +
            (!activitiesHtml.isBlank() ? "<div class='section-card'><div class='section-title'><i class='bi bi-clock'></i>Programme Schedule</div>" + activitiesHtml + "</div>" : "") +
            (address != null || !dateStr.isBlank() ? "<div class='section-card'><div class='section-title'><i class='bi bi-geo-alt'></i>Venue &amp; Date</div><div class='row g-3'>" +
            (!dateStr.isBlank() ? "<div class='col-sm-6'><div class='text-muted small mb-1'>Date</div><div class='fw-semibold'>" + dateStr + "</div></div>" : "") +
            (address != null ? "<div class='col-sm-6'><div class='text-muted small mb-1'>Address</div><div class='fw-medium'>" + address + "</div>" + (postcode != null ? "<div class='text-muted small mt-1'>" + postcode + "</div>" : "") + "</div>" : "") +
            "</div></div>" : "") +
            (about != null ? "<div class='section-card'><div class='section-title'><i class='bi bi-info-circle'></i>About the Event</div><div class='about-text'>" + about + "</div></div>" : "") +
            "<div class='inv-footer'>Invitation by <strong>" + orgName + "</strong></div></div>" +
            "<div class='share-bar'><span style='font-size:0.75rem;color:#94a3b8;white-space:nowrap'>Share:</span>" +
            "<span class='share-url' id='shareUrl'></span>" +
            "<button class='btn-copy' id='copyBtn' onclick='copyLink()'><i class='bi bi-clipboard'></i> Copy Link</button>" +
            "<a id='waBtn' href='#' target='_blank' rel='noopener' style='background:#25d366;color:#fff;border:none;border-radius:8px;padding:7px 14px;font-size:0.82rem;font-weight:600;text-decoration:none;display:flex;align-items:center;gap:6px;'><i class='bi bi-whatsapp'></i> WhatsApp</a></div>" +
            "<script src='https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js'></script>" +
            "<script>(function(){var u=window.location.href;document.getElementById('shareUrl').textContent=u;" +
            "document.getElementById('waBtn').href='https://wa.me/?text='+encodeURIComponent('You are invited to: " + title.replace("'", "\\'") + "\\n\\n'+u);})();" +
            "function copyLink(){var u=window.location.href,b=document.getElementById('copyBtn');" +
            "navigator.clipboard.writeText(u).then(function(){b.innerHTML='<i class=\"bi bi-check-lg\"></i> Copied!';b.classList.add('copied');setTimeout(function(){b.innerHTML='<i class=\"bi bi-clipboard\"></i> Copy Link';b.classList.remove('copied');},2500);})" +
            ".catch(function(){var t=document.createElement('textarea');t.value=u;document.body.appendChild(t);t.select();document.execCommand('copy');document.body.removeChild(t);});}</script>" +
            "</body></html>";
    }

    private String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes attrs) {
        service.deleteById(id);
        attrs.addFlashAttribute("success", "Invitation deleted.");
        return "redirect:/invitations";
    }
}
