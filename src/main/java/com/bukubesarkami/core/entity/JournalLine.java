package com.bukubesarkami.core.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "journal_lines")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JournalLine {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_entry_id", nullable = false)
    private JournalEntry journalEntry;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(name = "debit_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal debitAmount;

    @Column(name = "credit_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal creditAmount;

    @Column(length = 255)
    private String description;

    @Column(name = "line_order", nullable = false)
    private Integer lineOrder;

    @PrePersist
    protected void onCreate() {
        if (debitAmount == null) debitAmount = BigDecimal.ZERO;
        if (creditAmount == null) creditAmount = BigDecimal.ZERO;
    }
}