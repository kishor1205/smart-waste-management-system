package com.smartwaste.backend.service;

import com.smartwaste.backend.model.Complaint;
import com.smartwaste.backend.repository.ComplaintRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ComplaintService {

    @Autowired
    private ComplaintRepository repository;

    // 🔹 SAVE COMPLAINT
    public Complaint saveComplaint(Complaint complaint) {
        complaint.setStatus("Pending");

        // AI Priority Logic
        String desc = complaint.getDescription().toLowerCase();

        if (desc.contains("overflow") || desc.contains("urgent") || desc.contains("smell")) {
            complaint.setPriority("HIGH");
        } else if (desc.contains("garbage") || desc.contains("waste")) {
            complaint.setPriority("MEDIUM");
        } else {
            complaint.setPriority("LOW");
        }

        return repository.save(complaint);
    }

    // 🔹 GET ALL
    public List<Complaint> getAllComplaints() {
        return repository.findAll();
    }

    // 🔹 UPDATE STATUS
    public Complaint updateStatus(Long id, String status) {
        Complaint complaint = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Complaint not found"));

        complaint.setStatus(status);
        return repository.save(complaint);
    }

    // 🔥 HOTSPOTS
    public List<Object[]> getHotspots() {
        return repository.getHotspots();
    }

    public void deleteComplaint(Long id) {
        repository.deleteById(id);
    }
}