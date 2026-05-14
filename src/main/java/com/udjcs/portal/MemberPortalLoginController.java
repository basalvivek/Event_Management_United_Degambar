package com.udjcs.portal;

import com.udjcs.member.Member;
import com.udjcs.member.MemberRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;

@Controller
public class MemberPortalLoginController {

    private final MemberRepository memberRepository;

    public MemberPortalLoginController(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @GetMapping("/member-login")
    public String loginPage(HttpSession session) {
        if (Boolean.TRUE.equals(session.getAttribute("memberLoggedIn"))) {
            return "redirect:/portal";
        }
        return "member-login";
    }

    @PostMapping("/member-login")
    public String login(@RequestParam String email,
                        @RequestParam String password,
                        HttpSession session,
                        Model model) {
        Optional<Member> opt = memberRepository.findByEmail(email);
        if (opt.isPresent()) {
            Member member = opt.get();
            if ("Approved".equals(member.getApprovalStatus())
                    && member.getPassword() != null
                    && member.getPassword().equals(password)) {
                session.setAttribute("memberLoggedIn", Boolean.TRUE);
                session.setAttribute("memberUser", member);
                return "redirect:/portal";
            }
        }
        model.addAttribute("error", "Invalid credentials or account not approved.");
        return "member-login";
    }

    @PostMapping("/member-logout")
    public String logout(HttpSession session) {
        session.removeAttribute("memberLoggedIn");
        session.removeAttribute("memberUser");
        return "redirect:/member-login";
    }
}
