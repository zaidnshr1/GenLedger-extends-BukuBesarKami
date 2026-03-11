package com.bukubesarkami.core.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "accounts")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "account_code", nullable = false, unique = true, length = 20)
    private String accountCode;

    @Column(name = "account_name", nullable = false, length = 100)
    private String accountName;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 20)
    private AccountType accountType;

    @Enumerated(EnumType.STRING)
    @Column(name = "normal_balance", nullable = false, length = 10)
    private NormalBalance normalBalance;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project; // NULL = akun global kantor pusat

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
    }

    public enum AccountType {
        ASSET, LIABILITY, EQUITY, REVENUE, EXPENSE
    }

    public enum NormalBalance {
        DEBIT, CREDIT
    }
}