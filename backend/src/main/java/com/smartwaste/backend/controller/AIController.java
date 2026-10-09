package com.smartwaste.backend.controller;

import com.smartwaste.backend.service.GeminiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ai")
@CrossOrigin
public class AIController {

    @Autowired
    private GeminiService geminiService;

    // Analyze a complaint using its category and description.
    @GetMapping("/analyze")
    public String analyze(
            @RequestParam String complaint,
            @RequestParam(defaultValue = "Other Civic Issue") String category
    ) {
        return geminiService.analyzeComplaint(category, complaint);
    }
}