package com.smartwaste.backend.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

import java.util.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class GeminiService {

    // Keep your Gemini API key here.
    // Replace YOUR_API_KEY with your actual key.
    private final String API_KEY = System.getenv("GEMINI_API_KEY");
    public String analyzeComplaint(String category, String description) {

        if (API_KEY == null || API_KEY.isBlank()) {
            throw new IllegalStateException(
                    "GEMINI_API_KEY environment variable is not configured"
            );
        }

        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + API_KEY;
        RestTemplate restTemplate = new RestTemplate();

        // Category-aware prompt
        String prompt =
                "You are an AI assistant for a civic complaint management system.\n\n"
                        + "Complaint category: " + category + "\n"
                        + "Citizen's complaint: " + description + "\n\n"
                        + "Analyze the actual issue described by the citizen.\n"
                        + "IMPORTANT RULES:\n"
                        + "1. Focus on the selected complaint category and description.\n"
                        + "2. Do not assume every complaint is about garbage or waste.\n"
                        + "3. For Pothole / Road Damage, analyze road conditions and "
                        + "possible accident risks.\n"
                        + "4. For Waterlogging / Flooding, analyze flooding and "
                        + "possible public-safety impacts.\n"
                        + "5. For Fallen Tree, analyze obstruction and potential hazards.\n"
                        + "6. For Drainage Problem, analyze blocked drains and "
                        + "possible sewage or water-flow issues.\n"
                        + "7. For Bus Stop Problem, analyze infrastructure and "
                        + "passenger-safety concerns.\n"
                        + "8. For Garbage / Waste, analyze waste accumulation and "
                        + "sanitation concerns.\n"
                        + "9. For Other Civic Issue, analyze the actual problem described.\n"
                        + "10. Do not invent facts that the citizen has not provided.\n\n"
                        + "Return a concise analysis using these headings:\n"
                        + "Issue Summary:\n"
                        + "Potential Impact:\n"
                        + "Recommended Action:\n"
                        + "Suggested Urgency:\n";

        Map<String, Object> request = new HashMap<>();
        Map<String, Object> content = new HashMap<>();
        Map<String, String> part = new HashMap<>();

        part.put("text", prompt);

        content.put("parts", List.of(part));
        request.put("contents", List.of(content));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity =
                new HttpEntity<>(request, headers);

        ObjectMapper mapper = new ObjectMapper();

        // Retry twice if the request fails.
        for (int i = 0; i < 2; i++) {
            try {
                ResponseEntity<String> response =
                        restTemplate.postForEntity(
                                url,
                                entity,
                                String.class
                        );

                String body = response.getBody();

                if (body == null || body.isEmpty()) {
                    throw new RuntimeException("Empty response from Gemini");
                }

                JsonNode root = mapper.readTree(body);
                JsonNode candidates = root.path("candidates");

                if (!candidates.isArray() || candidates.isEmpty()) {
                    throw new RuntimeException(
                            "Gemini returned no analysis: "
                                    + root.path("promptFeedback").toString()
                    );
                }

                String result = candidates.get(0)
                        .path("content")
                        .path("parts")
                        .get(0)
                        .path("text")
                        .asText("");

                if (!result.isEmpty()) {
                    return result
                            .replace("\\n", "\n")
                            .replace("**", "");
                }

                throw new RuntimeException("Gemini returned empty analysis");

            } catch (Exception e) {
                System.err.println(
                        "Gemini attempt " + (i + 1)
                                + " failed: " + e.getMessage()
                );

                if (i == 0) {
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException interruptedException) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        // Category-specific fallback if Gemini is unavailable.
        return generateFallback(category, description);
    }

    private String generateFallback(String category, String description) {

        String normalizedCategory = category == null
                ? ""
                : category.toLowerCase(Locale.ROOT);

        String normalizedDescription = description == null
                ? ""
                : description.toLowerCase(Locale.ROOT);

        String issueSummary;
        String potentialImpact;
        String recommendedAction;

        if (normalizedCategory.contains("pothole")
                || normalizedCategory.contains("road damage")) {

            issueSummary = "Road damage or pothole reported.";
            potentialImpact =
                    "May affect road users and vehicle safety, depending on severity.";
            recommendedAction =
                    "Request the road maintenance team to inspect and repair the affected area.";

        } else if (normalizedCategory.contains("waterlogging")
                || normalizedCategory.contains("flooding")) {

            issueSummary = "Waterlogging or flooding reported.";
            potentialImpact =
                    "May disrupt movement and create safety concerns depending on water depth.";
            recommendedAction =
                    "Request an inspection of the affected area and appropriate drainage measures.";

        } else if (normalizedCategory.contains("fallen tree")) {

            issueSummary = "A fallen tree has been reported.";
            potentialImpact =
                    "May obstruct roads or pathways and could create hazards.";
            recommendedAction =
                    "Request the appropriate municipal team to inspect and safely remove the obstruction if necessary.";

        } else if (normalizedCategory.contains("drainage")) {

            issueSummary = "A drainage problem has been reported.";
            potentialImpact =
                    "May cause water accumulation, unpleasant odours, or sanitation concerns.";
            recommendedAction =
                    "Request the drainage maintenance team to inspect and clear the affected drain.";

        } else if (normalizedCategory.contains("bus stop")) {

            issueSummary = "A bus stop infrastructure problem has been reported.";
            potentialImpact =
                    "May inconvenience passengers or create safety concerns.";
            recommendedAction =
                    "Request the responsible team to inspect and repair the bus stop.";

        } else if (normalizedCategory.contains("garbage")
                || normalizedCategory.contains("waste")) {

            issueSummary = "A garbage or waste management issue has been reported.";
            potentialImpact =
                    "Accumulated waste may cause unpleasant odours and sanitation concerns.";
            recommendedAction =
                    "Request the sanitation team to inspect and arrange appropriate waste collection.";

        } else {

            issueSummary = "A civic issue has been reported.";
            potentialImpact =
                    "The impact depends on the specific issue and its severity.";
            recommendedAction =
                    "Request the relevant municipal team to inspect the reported issue.";
        }

        return "Issue Summary:\n"
                + issueSummary
                + "\n\nPotential Impact:\n"
                + potentialImpact
                + "\n\nRecommended Action:\n"
                + recommendedAction
                + "\n\nSuggested Urgency:\n"
                + assessUrgency(normalizedDescription);
    }

    private String assessUrgency(String description) {

        if (description.contains("accident")
                || description.contains("injury")
                || description.contains("dangerous")
                || description.contains("emergency")
                || description.contains("blocking road")
                || description.contains("severe flooding")) {

            return "HIGH - Prompt assessment is recommended based on the reported risk.";
        }

        return "MEDIUM - Inspect the reported issue and prioritize according to its actual severity.";
    }
}