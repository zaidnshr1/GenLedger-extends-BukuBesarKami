package com.bukubesarkami.core.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "projects")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "project_code", nullable = false, unique = true, length = 20)
    private String projectCode;

    @Column(name = "project_name", nullable = false, length = 100)
    private String projectName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal budget;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
        if (budget == null) budget = BigDecimal.ZERO;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}