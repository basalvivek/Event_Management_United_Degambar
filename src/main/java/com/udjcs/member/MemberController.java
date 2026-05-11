package com.udjcs.member;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
}
