package com.udjcs.aiagent;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/ai-agent")
public class AIAgentController {

    private final AIAgentService aiAgentService;

    public AIAgentController(AIAgentService aiAgentService) {
        this.aiAgentService = aiAgentService;
    }

    @GetMapping
    public String index(Model model) {
        return "ai-agent/index";
    }

    @PostMapping("/ask")
    @ResponseBody
    public String ask(@RequestParam("question") String question) {
        return aiAgentService.ask(question.trim());
    }
}
