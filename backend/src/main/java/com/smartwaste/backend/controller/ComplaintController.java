package com.smartwaste.backend.controller;

import com.smartwaste.backend.model.Complaint;
import com.smartwaste.backend.service.ComplaintService;
import com.smartwaste.backend.service.GeminiService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/complaints")
@CrossOrigin
public class ComplaintController {

    @Autowired
    private ComplaintService service;

    @Autowired
    private GeminiService geminiService;

    // =========================================
    // 1. CREATE COMPLAINT
    // Android app and web application
    // =========================================
    @PostMapping
    public Complaint addComplaint(@RequestBody Complaint complaint) {

        String category = normalizeCategory(complaint.getCategory());

        complaint.setCategory(category);

        String description = complaint.getDescription() == null
                ? ""
                : complaint.getDescription().trim();

        complaint.setDescription(description);

        // Generate category-aware AI analysis.
        String aiResult = geminiService.analyzeComplaint(
                category,
                description
        );

        complaint.setAiResult(aiResult);

        // Assign priority after AI analysis.
        setPriority(complaint, aiResult);

        complaint.setStatus("PENDING");

        System.out.println(
                "Complaint category: " + category
                        + " | Priority assigned: " + complaint.getPriority()
        );

        return service.saveComplaint(complaint);
    }

    // =========================================
    // 2. IMAGE UPLOAD
    // =========================================
    @PostMapping("/upload")
    public Complaint uploadComplaint(
            @RequestParam("description") String description,
            @RequestParam("location") String location,
            @RequestParam("image") MultipartFile image,
            @RequestParam(
                    value = "category",
                    defaultValue = "Other Civic Issue"
            ) String category
    ) {

        try {
            String uploadDir = "uploads/";

            java.io.File dir = new java.io.File(uploadDir);

            if (!dir.exists() && !dir.mkdirs()) {
                throw new RuntimeException("Could not create uploads directory");
            }

            String originalName = image.getOriginalFilename();

            String extension = ".jpg";

            if (originalName != null && originalName.contains(".")) {
                extension = originalName.substring(
                        originalName.lastIndexOf(".")
                );
            }

            String fileName = System.currentTimeMillis() + extension;

            Path filePath = Paths.get(uploadDir, fileName);

            Files.write(filePath, image.getBytes());

            String normalizedCategory = normalizeCategory(category);

            String cleanedDescription = description == null
                    ? ""
                    : description.trim();

            String aiResult = geminiService.analyzeComplaint(
                    normalizedCategory,
                    cleanedDescription
            );

            Complaint complaint = new Complaint();

            complaint.setCategory(normalizedCategory);
            complaint.setDescription(cleanedDescription);
            complaint.setLocation(location);

            complaint.setImageUrl(
                    "http://localhost:8080/uploads/" + fileName
            );

            complaint.setStatus("PENDING");
            complaint.setAiResult(aiResult);

            setPriority(complaint, aiResult);

            System.out.println(
                    "Uploaded complaint category: " + normalizedCategory
                            + " | Priority assigned: " + complaint.getPriority()
            );

            return service.saveComplaint(complaint);

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Image upload failed");
        }
    }

    // =========================================
    // 3. NORMALIZE CATEGORY
    // =========================================
    private String normalizeCategory(String category) {

        if (category == null || category.trim().isEmpty()) {
            return "Other Civic Issue";
        }

        return category.trim();
    }

    // =========================================
    // 4. PRIORITY CLASSIFICATION
    // =========================================
    private void setPriority(Complaint complaint, String aiResult) {

        String category = complaint.getCategory() == null
                ? ""
                : complaint.getCategory().toLowerCase(Locale.ROOT);

        String description = complaint.getDescription() == null
                ? ""
                : complaint.getDescription().toLowerCase(Locale.ROOT);

        String analysis = aiResult == null
                ? ""
                : aiResult.toLowerCase(Locale.ROOT);

        String combined = category + " " + description + " " + analysis;

        /*
         * STEP 1:
         * Apply clear safety rules before interpreting Gemini's urgency.
         */

        if (containsAny(
                combined,
                "risk to life",
                "electric shock",
                "exposed wire",
                "live power line",
                "blocking emergency access",
                "severe flooding",
                "sewage overflow",
                "contaminated water",
                "immediate traffic hazard",
                "risk of traffic accidents",
                "increased risk of traffic accidents",
                "vehicles to swerve",
                "blocking traffic",
                "road completely blocked"
        )) {
            complaint.setPriority("HIGH");
            return;
        }

        /*
         * STEP 2:
         * Extract the urgency from Gemini's analysis.
         *
         * Supports examples:
         * Suggested Urgency: High
         * Suggested Urgency: High - due to safety hazards
         * Suggested Urgency: Moderate
         * Suggested Urgency: Moderate to High
         * Suggested Urgency: Low (pending more information)
         */

        Pattern urgencyPattern = Pattern.compile(
                "(?im)Suggested\\s+Urgency\\s*:\\s*"
                        + "(HIGH|MEDIUM|MODERATE|LOW)"
                        + "(?:\\s+to\\s+(HIGH|MEDIUM|MODERATE|LOW))?\\b"
        );

        Matcher matcher = urgencyPattern.matcher(analysis);

        if (matcher.find()) {

            String first = matcher.group(1).toUpperCase(Locale.ROOT);
            String second = matcher.group(2);

            // Handle urgency ranges.
            if (second != null) {

                second = second.toUpperCase(Locale.ROOT);

                if (first.equals("HIGH") || second.equals("HIGH")) {
                    complaint.setPriority("HIGH");
                } else if (first.equals("MEDIUM")
                        || first.equals("MODERATE")
                        || second.equals("MEDIUM")
                        || second.equals("MODERATE")) {
                    complaint.setPriority("MEDIUM");
                } else {
                    complaint.setPriority("LOW");
                }

                return;
            }

            if (first.equals("HIGH")) {
                complaint.setPriority("HIGH");
                return;
            }

            if (first.equals("MEDIUM") || first.equals("MODERATE")) {
                complaint.setPriority("MEDIUM");
                return;
            }

            if (first.equals("LOW")) {
                complaint.setPriority("LOW");
                return;
            }
        }

        /*
         * STEP 3:
         * Category-specific fallback when Gemini's urgency
         * cannot be extracted.
         */

        // Potholes and road damage.
        if (category.contains("pothole")
                || category.contains("road damage")) {

            if (containsAny(
                    combined,
                    "deep pothole",
                    "large pothole",
                    "major pothole",
                    "school",
                    "busy road",
                    "vehicles to swerve",
                    "traffic hazard",
                    "accident"
            )) {
                complaint.setPriority("HIGH");
            } else {
                complaint.setPriority("MEDIUM");
            }

            return;
        }

        // Fallen trees.
        if (category.contains("fallen tree")) {

            if (containsAny(
                    combined,
                    "blocking road",
                    "road blocked",
                    "blocking traffic",
                    "power line",
                    "live wire",
                    "electrical hazard"
            )) {
                complaint.setPriority("HIGH");
            } else {
                complaint.setPriority("MEDIUM");
            }

            return;
        }

        // Waterlogging and flooding.
        if (category.contains("waterlogging")
                || category.contains("flooding")) {

            if (containsAny(
                    combined,
                    "severe flooding",
                    "deep water",
                    "blocking traffic",
                    "vehicles cannot pass",
                    "dangerous current",
                    "water entering houses"
            )) {
                complaint.setPriority("HIGH");
            } else {
                complaint.setPriority("MEDIUM");
            }

            return;
        }

        // Drainage problems.
        if (category.contains("drainage")) {

            if (containsAny(
                    combined,
                    "sewage overflow",
                    "sewage on road",
                    "contaminated water",
                    "open manhole",
                    "exposed drain",
                    "health hazard"
            )) {
                complaint.setPriority("HIGH");
            } else {
                complaint.setPriority("MEDIUM");
            }

            return;
        }

        // Preserve category-specific waste behaviour.
        if (category.contains("garbage")
                || category.contains("waste")) {

            if (containsAny(
                    combined,
                    "overflow",
                    "overflowing",
                    "bad smell",
                    "health hazard",
                    "contamination"
            )) {
                complaint.setPriority("HIGH");
            } else {
                complaint.setPriority("MEDIUM");
            }

            return;
        }

        // Bus stop problems.
        if (category.contains("bus stop")) {

            if (containsAny(
                    combined,
                    "structural collapse",
                    "collapse risk",
                    "exposed wire",
                    "electric shock",
                    "unsafe structure"
            )) {
                complaint.setPriority("HIGH");
            } else {
                complaint.setPriority("MEDIUM");
            }

            return;
        }

        // Default for other civic issues.
        complaint.setPriority("LOW");
    }

    // =========================================
    // 5. KEYWORD HELPER
    // =========================================
    private boolean containsAny(String text, String... keywords) {

        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }

        return false;
    }

    // =========================================
    // 6. GET ALL COMPLAINTS
    // =========================================
    @GetMapping
    public List<Complaint> getAll() {
        return service.getAllComplaints();
    }

    // =========================================
    // 7. UPDATE STATUS
    // =========================================
    @PutMapping("/{id}/status")
    public Complaint updateStatus(
            @PathVariable Long id,
            @RequestParam String status
    ) {
        return service.updateStatus(id, status);
    }

    // =========================================
    // 8. HOTSPOTS
    // =========================================
    @GetMapping("/hotspots")
    public List<Object[]> hotspots() {
        return service.getHotspots();
    }

    // =========================================
    // 9. DELETE COMPLAINT
    // =========================================
    @DeleteMapping("/{id}")
    public String deleteComplaint(@PathVariable Long id) {
        service.deleteComplaint(id);
        return "Deleted Successfully";
    }
}