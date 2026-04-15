package com.smartwaste.backend.controller;

import com.smartwaste.backend.model.Complaint;
import com.smartwaste.backend.service.ComplaintService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/complaints")
@CrossOrigin
public class ComplaintController {

    @Autowired
    private ComplaintService service;

    // 🔹 POST
    @PostMapping
    public Complaint addComplaint(@RequestBody Complaint complaint) {
        return service.saveComplaint(complaint);
    }

    // 🔹 GET ALL
    @GetMapping
    public List<Complaint> getAll() {
        return service.getAllComplaints();
    }

    // 🔹 UPDATE STATUS
    @PutMapping("/{id}/status")
    public Complaint updateStatus(@PathVariable Long id, @RequestParam String status) {
        return service.updateStatus(id, status);
    }

    // 🔥 HOTSPOTS
    @GetMapping("/hotspots")
    public List<Object[]> hotspots() {
        return service.getHotspots();
    }

    @DeleteMapping("/{id}")
    public String deleteComplaint(@PathVariable Long id) {
        service.deleteComplaint(id);
        return "Deleted Successfully";
    }
}