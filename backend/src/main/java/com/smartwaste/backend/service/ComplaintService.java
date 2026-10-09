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

    // SAVE COMPLAINT
    public Complaint saveComplaint(Complaint complaint) {

        // Set the default status only if it is missing.
        if (complaint.getStatus() == null
                || complaint.getStatus().trim().isEmpty()) {
            complaint.setStatus("PENDING");
        }

        // IMPORTANT:
        // Do not calculate priority here.
        // ComplaintController has already assigned the priority
        // using Gemini analysis and category-specific rules.

        return repository.save(complaint);
    }

    // GET ALL COMPLAINTS
    public List<Complaint> getAllComplaints() {
        return repository.findAll();
    }

    // UPDATE STATUS
    public Complaint updateStatus(Long id, String status) {

        Complaint complaint = repository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Complaint not found"));

        complaint.setStatus(status);

        return repository.save(complaint);
    }

    // HOTSPOTS
    public List<Object[]> getHotspots() {
        return repository.getHotspots();
    }

    // DELETE COMPLAINT
    public void deleteComplaint(Long id) {
        repository.deleteById(id);
    }
}