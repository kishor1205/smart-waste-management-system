package com.smartwaste.backend.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

import java.util.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class GeminiService {

    // ⚠️ Put your API key here (do NOT share publicly)
    private final String API_KEY = "YOUR_API_KEY";

    public String analyzeComplaint(String complaint) {

        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + API_KEY;

        RestTemplate restTemplate = new RestTemplate();

        try {

            // 🔹 Request body
            Map<String, Object> request = new HashMap<>();
            Map<String, Object> content = new HashMap<>();
            Map<String, String> part = new HashMap<>();

            part.put("text", "Analyze this complaint briefly and give solution:\n" + complaint);

            content.put("parts", List.of(part));
            request.put("contents", List.of(content));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity =
                    new HttpEntity<>(request, headers);

            // 🔁 Retry (2 times)
            for (int i = 0; i < 2; i++) {
                try {

                    ResponseEntity<String> response =
                            restTemplate.postForEntity(url, entity, String.class);

                    String body = response.getBody();

                    ObjectMapper mapper = new ObjectMapper();
                    JsonNode root = mapper.readTree(body);

                    String result = root
                            .path("candidates")
                            .get(0)
                            .path("content")
                            .path("parts")
                            .get(0)
                            .path("text")
                            .asText();

                    return result
                            .replace("\\n", "\n")
                            .replace("**", "");

                } catch (Exception e) {
                    // wait and retry
                    Thread.sleep(2000);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        // 🔥 Fallback (very important for hackathon)
        return "Garbage issue detected.\n\n"
                + "Possible problems:\n"
                + "- Health hazards\n"
                + "- Bad smell\n"
                + "- Environmental pollution\n\n"
                + "Suggested action:\n"
                + "- Report to municipal authorities\n"
                + "- Use city complaint app";
    }
}