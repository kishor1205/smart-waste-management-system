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

@RestController
@RequestMapping("/api/complaints")
@CrossOrigin
public class ComplaintController {

    @Autowired
    private ComplaintService service;

    @Autowired
    private GeminiService geminiService;

    // =========================================
    // 🔹 1. NORMAL POST (from web index.html)
    // =========================================
    @PostMapping
    public Complaint addComplaint(@RequestBody Complaint complaint) {

        // 🔥 AI
        String aiResult = geminiService.analyzeComplaint(complaint.getDescription());
        complaint.setAiResult(aiResult);

        setPriority(complaint, aiResult);

        complaint.setStatus("PENDING");

        return service.saveComplaint(complaint);
    }


    // =========================================
    // 🔥 2. IMAGE UPLOAD (from Android app)
    // =========================================
    @PostMapping("/upload")
    public Complaint uploadComplaint(
            @RequestParam("description") String description,
            @RequestParam("location") String location,
            @RequestParam("image") MultipartFile image
    ) {
        try {

            // 📁 Create uploads folder
            String uploadDir = "uploads/";
            java.io.File dir = new java.io.File(uploadDir);
            if (!dir.exists()) dir.mkdirs();

            // 📸 Save image
            String fileName = System.currentTimeMillis() + "_" + image.getOriginalFilename();
            Path filePath = Paths.get(uploadDir + fileName);
            Files.write(filePath, image.getBytes());

            // 🧠 AI
            String aiResult = geminiService.analyzeComplaint(description);

            // 🧾 Create complaint
            Complaint complaint = new Complaint();
            complaint.setDescription(description);
            complaint.setLocation(location);
            complaint.setImageUrl("http://localhost:8080/uploads/" + fileName);
            complaint.setStatus("PENDING");
            complaint.setAiResult(aiResult);

            setPriority(complaint, aiResult);

            return service.saveComplaint(complaint);

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Image upload failed");
        }
    }


    // =========================================
    // 🔹 COMMON PRIORITY LOGIC
    // =========================================
    private void setPriority(Complaint complaint, String aiResult) {

        if (aiResult == null) {
            complaint.setPriority("MEDIUM");
            return;
        }

        String lower = aiResult.toLowerCase();

        if (lower.contains("health") || lower.contains("disease") || lower.contains("danger")) {
            complaint.setPriority("HIGH");
        } else if (lower.contains("smell") || lower.contains("mosquito")) {
            complaint.setPriority("MEDIUM");
        } else {
            complaint.setPriority("LOW");
        }
    }


    // =========================================
    // 🔹 GET ALL
    // =========================================
    @GetMapping
    public List<Complaint> getAll() {
        return service.getAllComplaints();
    }


    // =========================================
    // 🔹 UPDATE STATUS
    // =========================================
    @PutMapping("/{id}/status")
    public Complaint updateStatus(@PathVariable Long id, @RequestParam String status) {
        return service.updateStatus(id, status);
    }


    // =========================================
    // 🔥 HOTSPOTS
    // =========================================
    @GetMapping("/hotspots")
    public List<Object[]> hotspots() {
        return service.getHotspots();
    }


    // =========================================
    // 🔹 DELETE
    // =========================================
    @DeleteMapping("/{id}")
    public String deleteComplaint(@PathVariable Long id) {
        service.deleteComplaint(id);
        return "Deleted Successfully";
    }
}