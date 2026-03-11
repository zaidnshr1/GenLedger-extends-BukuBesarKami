package com.bukubesarkami.core.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "journal_entries")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JournalEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "entry_number", nullable = false, unique = true, length = 30)
    private String entryNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project; // NULL = transaksi kantor pusat

    @Column(name = "entry_date", nullable = false)
    private LocalDate entryDate;

    @Column(nullable = false, length = 255)
    private String description;

    @Column(name = "reference_number", length = 50)
    private String referenceNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;

    @Column(name = "total_debit", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalDebit;

    @Column(name = "total_credit", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalCredit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "posted_by")
    private User postedBy;

    @Column(name = "posted_at")
    private OffsetDateTime postedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voided_by")
    private User voidedBy;

    @Column(name = "voided_at")
    private OffsetDateTime voidedAt;

    @Column(name = "void_reason", length = 255)
    private String voidReason;

    @OneToMany(mappedBy = "journalEntry", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("lineOrder ASC")
    @Builder.Default
    private List<JournalLine> lines = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
        if (status == null) status = Status.DRAFT;
        if (totalDebit == null) totalDebit = BigDecimal.ZERO;
        if (totalCredit == null) totalCredit = BigDecimal.ZERO;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public boolean isBalanced() {
        return totalDebit.compareTo(totalCredit) == 0;
    }

    public enum Status {
        DRAFT, POSTED, VOIDED
    }
}