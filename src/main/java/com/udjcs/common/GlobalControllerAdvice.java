package com.udjcs.common;

import com.udjcs.eventprogram.EventProgramService;
import com.udjcs.gallery.GalleryService;
import com.udjcs.organization.Organization;
import com.udjcs.organization.OrganizationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.ui.Model;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.beans.PropertyEditorSupport;
import java.io.IOException;
import java.net.InetAddress;
import java.time.LocalDate;
import java.util.List;

@ControllerAdvice
public class GlobalControllerAdvice {

    @Value("${app.base.url:}")
    private String configuredBaseUrl;

    private final EventProgramService eventProgramService;
    private final OrganizationService organizationService;
    private final GalleryService galleryService;

    public GlobalControllerAdvice(EventProgramService eventProgramService,
                                  OrganizationService organizationService,
                                  GalleryService galleryService) {
        this.eventProgramService = eventProgramService;
        this.organizationService = organizationService;
        this.galleryService = galleryService;
    }

    public String resolveBaseUrl(HttpServletRequest request) {
        if (configuredBaseUrl != null && !configuredBaseUrl.isBlank()) {
            return configuredBaseUrl.replaceAll("/$", "");
        }
        String scheme = request.getScheme();
        int port = request.getServerPort();
        String host = request.getServerName();
        if ("localhost".equals(host) || "127.0.0.1".equals(host)) {
            try {
                String ip = InetAddress.getLocalHost().getHostAddress();
                if (ip != null && !ip.startsWith("127.")) host = ip;
            } catch (Exception ignored) {}
        }
        return scheme + "://" + host + ":" + port;
    }

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));
        binder.registerCustomEditor(LocalDate.class, new PropertyEditorSupport() {
            @Override
            public void setAsText(String text) {
                if (text == null || text.trim().isEmpty()) {
                    setValue(null);
                } else {
                    setValue(LocalDate.parse(text));
                }
            }
        });
    }

    @ModelAttribute
    public void addGlobalAttributes(HttpServletRequest request, Model model) {
        model.addAttribute("currentUri", request.getRequestURI());
        model.addAttribute("baseUrl", resolveBaseUrl(request));
        HttpSession session = request.getSession(false);
        model.addAttribute("adminUser", session != null ? session.getAttribute("adminUser") : null);
        model.addAttribute("memberUser", session != null ? session.getAttribute("memberUser") : null);
        boolean loggedIn = session != null && Boolean.TRUE.equals(session.getAttribute("loggedIn"));
        model.addAttribute("activePrograms",
                loggedIn ? eventProgramService.findByActiveEvents() : List.of());

        java.util.List<Organization> orgs = organizationService.findAll();
        Organization siteOrg = orgs.isEmpty() ? null : orgs.get(0);
        model.addAttribute("siteOrg", siteOrg);
        model.addAttribute("siteOrgName", siteOrg != null ? siteOrg.getName() : "UDJCS");
        model.addAttribute("galleryImageIds", galleryService.findAllIds());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public String handleNotFound(IllegalArgumentException ex, RedirectAttributes attrs) {
        attrs.addFlashAttribute("error", ex.getMessage());
        return "redirect:/";
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public String handleForeignKey(HttpServletRequest request, RedirectAttributes attrs) {
        attrs.addFlashAttribute("error",
            "Cannot delete — this record is linked to other data. Remove the related records first.");
        String referer = request.getHeader("Referer");
        return "redirect:" + (referer != null && !referer.isEmpty() ? referer : "/");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String handleFileTooLarge(HttpServletRequest request, RedirectAttributes attrs) {
        attrs.addFlashAttribute("error", "File too large. Maximum allowed upload size is 10 MB.");
        String referer = request.getHeader("Referer");
        return "redirect:" + (referer != null && !referer.isEmpty() ? referer : "/");
    }

    @ExceptionHandler(IOException.class)
    public String handleIOException(IOException ex, RedirectAttributes attrs) {
        attrs.addFlashAttribute("error", "Could not process the uploaded file. Please use JPG or PNG format.");
        return "redirect:/";
    }
}
