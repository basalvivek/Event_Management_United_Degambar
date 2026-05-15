package com.udjcs.portal;

import com.udjcs.member.Member;
import com.udjcs.member.MemberRepository;
import com.udjcs.member.MemberService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

@Controller
@RequestMapping("/register")
public class PublicRegistrationController {

    private final MemberService memberService;
    private final MemberRepository memberRepository;

    public PublicRegistrationController(MemberService memberService,
                                        MemberRepository memberRepository) {
        this.memberService = memberService;
        this.memberRepository = memberRepository;
    }

    @GetMapping
    public String showForm() {
        return "register";
    }

    @PostMapping
    public String register(@RequestParam(required = false) String firstName,
                           @RequestParam(required = false) String lastName,
                           @RequestParam(required = false) String email,
                           @RequestParam(required = false) String phone,
                           @RequestParam(required = false) String gender,
                           @RequestParam(required = false) String address,
                           @RequestParam(required = false) String city,
                           @RequestParam(required = false) String state,
                           @RequestParam(required = false) String password,
                           @RequestParam(required = false) String confirmPassword,
                           Model model,
                           RedirectAttributes attrs) {

        if (firstName == null || lastName == null || email == null || phone == null
                || gender == null || address == null || city == null || state == null
                || password == null || confirmPassword == null) {
            model.addAttribute("error", "All fields are required.");
            repopulate(model, firstName, lastName, email, phone, gender, address, city, state);
            return "register";
        }
        if (!password.equals(confirmPassword)) {
            model.addAttribute("error", "Passwords do not match.");
            repopulate(model, firstName, lastName, email, phone, gender, address, city, state);
            return "register";
        }
        if (!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            model.addAttribute("error", "Enter a valid email address.");
            repopulate(model, firstName, lastName, email, phone, gender, address, city, state);
            return "register";
        }
        if (memberRepository.findFirstByEmailIgnoreCase(email.trim()).isPresent()) {
            model.addAttribute("error", "An account with this email already exists.");
            repopulate(model, firstName, lastName, email, phone, gender, address, city, state);
            return "register";
        }

        Member member = new Member();
        member.setFirstName(firstName.trim());
        member.setLastName(lastName.trim());
        member.setEmail(email.trim());
        member.setPhone(phone.trim());
        member.setGender(gender);
        member.setAddress(address.trim());
        member.setCity(city.trim());
        member.setState(state.trim());
        member.setPassword(password);
        member.setMembershipType("Unauthorised");
        member.setApprovalStatus("Pending");
        member.setMembershipDate(LocalDate.now());
        member.setStatus("Active");

        memberService.save(member);
        attrs.addFlashAttribute("success",
                "Registration successful! Please wait for admin approval before logging in.");
        return "redirect:/member-login";
    }

    private void repopulate(Model model, String firstName, String lastName, String email,
                            String phone, String gender, String address, String city, String state) {
        model.addAttribute("firstName", firstName);
        model.addAttribute("lastName", lastName);
        model.addAttribute("email", email);
        model.addAttribute("phone", phone);
        model.addAttribute("gender", gender);
        model.addAttribute("address", address);
        model.addAttribute("city", city);
        model.addAttribute("state", state);
    }
}
