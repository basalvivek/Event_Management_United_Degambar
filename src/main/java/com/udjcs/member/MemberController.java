package com.udjcs.member;

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

@Controller
@RequestMapping("/members")
public class MemberController {

    private final MemberService service;

    public MemberController(MemberService service) {
        this.service = service;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("items", service.findAll());
        return "member/list";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("item", new Member());
        return "member/form";
    }

    @PostMapping
    public String create(@ModelAttribute("item") @Valid Member member,
                         BindingResult result,
                         RedirectAttributes attrs) {
        if (result.hasErrors()) return "member/form";
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
        return "member/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @ModelAttribute("item") @Valid Member member,
                         BindingResult result,
                         RedirectAttributes attrs) {
        if (result.hasErrors()) return "member/form";
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
