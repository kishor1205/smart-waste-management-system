package com.smartwaste.backend.model;
import jakarta.persistence.*;
import lombok.*;


    @Entity
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public class Complaint {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        private String description;
        private String imageUrl;
        private String location;
        private String status;
        private String priority;
    }

