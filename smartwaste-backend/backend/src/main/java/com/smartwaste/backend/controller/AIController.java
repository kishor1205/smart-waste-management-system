package com.smartwaste.backend.controller;

import com.smartwaste.backend.service.GeminiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ai")
public class AIController {

    @Autowired
    private GeminiService geminiService;

    @GetMapping("/analyze")
    public String analyze(@RequestParam String complaint) {
        return geminiService.analyzeComplaint(complaint);
    }
}