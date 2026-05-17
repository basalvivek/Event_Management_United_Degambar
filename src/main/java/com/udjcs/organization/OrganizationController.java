package com.udjcs.organization;

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
import java.util.List;

@Controller
@RequestMapping("/organization")
public class OrganizationController {

    private final OrganizationService service;
    private final OrganizationDisplayPictureService displayPictureService;

    public OrganizationController(OrganizationService service,
                                   OrganizationDisplayPictureService displayPictureService) {
        this.service = service;
        this.displayPictureService = displayPictureService;
    }

    @GetMapping
    public String list() {
        List<Organization> all = service.findAll();
        if (all.isEmpty()) {
            return "redirect:/organization/new";
        }
        return "redirect:/organization/" + all.get(0).getId() + "/edit";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("item", new Organization());
        return "organization/form";
    }

    @PostMapping
    public String create(@ModelAttribute("item") @Valid Organization organization,
                         BindingResult result,
                         @RequestParam(value = "logoFile", required = false) MultipartFile logoFile,
                         RedirectAttributes attrs) throws IOException {
        if (result.hasErrors()) return "organization/form";
        service.save(organization, logoFile);
        attrs.addFlashAttribute("success", "Organization saved successfully.");
        return "redirect:/organization";
    }

    @GetMapping("/{id}")
    public String redirectToEdit(@PathVariable Long id) {
        return "redirect:/organization/" + id + "/edit";
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model) {
        model.addAttribute("item", service.findById(id));
        model.addAttribute("displayPictures", displayPictureService.findByOrganization(id));
        model.addAttribute("displayPictureCount", displayPictureService.countByOrganization(id));
        return "organization/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @ModelAttribute("item") @Valid Organization organization,
                         BindingResult result,
                         @RequestParam(value = "logoFile", required = false) MultipartFile logoFile,
                         RedirectAttributes attrs) throws IOException {
        if (result.hasErrors()) return "organization/form";
        organization.setId(id);
        service.save(organization, logoFile);
        attrs.addFlashAttribute("success", "Organization updated successfully.");
        return "redirect:/organization";
    }

    @PostMapping("/{id}/banner")
    public String uploadBanner(@PathVariable Long id,
                               @RequestParam("bannerFile") MultipartFile bannerFile,
                               RedirectAttributes attrs) throws IOException {
        if (bannerFile == null || bannerFile.isEmpty()) {
            attrs.addFlashAttribute("error", "Please select an image to upload.");
            return "redirect:/organization/" + id + "/edit";
        }
        String err = validateBanner(bannerFile);
        if (err != null) {
            attrs.addFlashAttribute("error", err);
            return "redirect:/organization/" + id + "/edit";
        }
        service.saveBanner(id, bannerFile);
        attrs.addFlashAttribute("success", "Banner uploaded successfully.");
        return "redirect:/organization/" + id + "/edit";
    }

    @PostMapping("/{id}/banner/delete")
    public String deleteBanner(@PathVariable Long id, RedirectAttributes attrs) {
        service.deleteBanner(id);
        attrs.addFlashAttribute("success", "Banner removed.");
        return "redirect:/organization/" + id + "/edit";
    }

    @GetMapping("/{id}/logo")
    @ResponseBody
    public ResponseEntity<byte[]> serveLogo(@PathVariable Long id) {
        Organization org = service.findById(id);
        if (org.getLogoData() == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(org.getLogoMimeType()))
                .body(org.getLogoData());
    }

    @GetMapping("/{id}/banner")
    @ResponseBody
    public ResponseEntity<byte[]> serveBanner(@PathVariable Long id) {
        Organization org = service.findById(id);
        if (org.getBannerImage() == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(org.getBannerMimeType()))
                .body(org.getBannerImage());
    }

    @PostMapping("/{orgId}/display-pictures")
    public String uploadDisplayPicture(@PathVariable Long orgId,
                                       @RequestParam("displayPicFile") MultipartFile file,
                                       RedirectAttributes attrs) throws IOException {
        if (file == null || file.isEmpty()) {
            attrs.addFlashAttribute("error", "Please select an image to upload.");
            return "redirect:/organization/" + orgId + "/edit";
        }
        String err = validateDisplayPicture(file);
        if (err != null) {
            attrs.addFlashAttribute("error", err);
            return "redirect:/organization/" + orgId + "/edit";
        }
        displayPictureService.upload(orgId, file);
        attrs.addFlashAttribute("success", "Display picture added.");
        return "redirect:/organization/" + orgId + "/edit";
    }

    @PostMapping("/display-pictures/{picId}/delete")
    public String deleteDisplayPicture(@PathVariable Long picId,
                                       @RequestParam("orgId") Long orgId,
                                       RedirectAttributes attrs) {
        displayPictureService.delete(picId);
        attrs.addFlashAttribute("success", "Display picture removed.");
        return "redirect:/organization/" + orgId + "/edit";
    }

    @GetMapping("/display-pictures/{picId}")
    @ResponseBody
    public ResponseEntity<byte[]> serveDisplayPicture(@PathVariable Long picId) {
        OrganizationDisplayPicture pic = displayPictureService.findById(picId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(pic.getMimeType()))
                .body(pic.getImageData());
    }

    private String validateDisplayPicture(MultipartFile file) {
        if (file.getSize() > 2_097_152) return "Image must be 2 MB or smaller.";
        String ct = file.getContentType();
        if (ct == null || (!ct.equals("image/jpeg") && !ct.equals("image/png") && !ct.equals("image/webp")))
            return "Image must be JPG, PNG or WEBP.";
        return null;
    }

    private String validateBanner(MultipartFile file) {
        if (file.getSize() > 2_097_152) return "Banner image must be 2 MB or smaller.";
        String ct = file.getContentType();
        if (ct == null || (!ct.equals("image/jpeg") && !ct.equals("image/png") && !ct.equals("image/webp")))
            return "Banner must be JPG, PNG or WEBP.";
        return null;
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes attrs) {
        service.deleteById(id);
        attrs.addFlashAttribute("success", "Organization deleted.");
        return "redirect:/organization";
    }
}
