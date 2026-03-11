package com.bukubesarkami.core.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_projects",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "project_id"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProject {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "assigned_at", nullable = false, updatable = false)
    private OffsetDateTime assignedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_by", nullable = false)
    private User assignedBy;

    @PrePersist
    protected void onCreate() {
        assignedAt = OffsetDateTime.now();
    }
}