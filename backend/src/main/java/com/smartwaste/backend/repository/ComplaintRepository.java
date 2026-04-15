package com.smartwaste.backend.repository;

import com.smartwaste.backend.model.Complaint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ComplaintRepository extends JpaRepository<Complaint, Long> {

    // 🔥 HOTSPOT QUERY
    @Query("SELECT c.location, COUNT(c) FROM Complaint c GROUP BY c.location")
    List<Object[]> getHotspots();
}