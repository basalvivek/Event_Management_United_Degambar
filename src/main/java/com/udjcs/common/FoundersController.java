package com.udjcs.common;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class FoundersController {

    @GetMapping("/founders")
    public String founders() {
        return "founders";
    }
}
