package com.bukubesarkami.features.adminproject.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public final class AdminProjectDto {

    private AdminProjectDto() {}

    // ===== JOURNAL ENTRY =====

    public record JournalLineRequest(
            @NotNull UUID accountId,
            @NotNull @DecimalMin("0") BigDecimal debitAmount,
            @NotNull @DecimalMin("0") BigDecimal creditAmount,
            @Size(max = 255) String description
    ) {}

    public record CreateJournalRequest(
            @NotNull UUID projectId,
            @NotNull LocalDate entryDate,
            @NotBlank @Size(max = 255) String description,
            @Size(max = 50) String referenceNumber,
            @NotNull @Size(min = 2) @Valid
            List<JournalLineRequest> lines
    ) {}

    public record UpdateJournalRequest(
            LocalDate entryDate,
            @Size(max = 255) String description,
            @Size(max = 50) String referenceNumber,
            @Size(min = 2) @Valid
            List<JournalLineRequest> lines
    ) {}

    public record JournalLineResponse(
            UUID id,
            UUID accountId, String accountCode, String accountName,
            BigDecimal debitAmount, BigDecimal creditAmount,
            String description, int lineOrder
    ) {}

    public record JournalEntryResponse(
            UUID id, String entryNumber, UUID projectId,
            LocalDate entryDate, String description, String referenceNumber,
            String status, BigDecimal totalDebit, BigDecimal totalCredit,
            List<JournalLineResponse> lines,
            UUID createdById, String createdByUsername,
            OffsetDateTime createdAt, OffsetDateTime updatedAt
    ) {}

    public record VoidJournalRequest(
            @NotBlank @Size(max = 255) String reason
    ) {}

    // ===== BUDGET & SUMMARY =====

    public record BudgetSummaryResponse(
            UUID projectId, String projectName,
            BigDecimal totalBudget,
            BigDecimal totalExpense,
            BigDecimal totalRevenue,
            BigDecimal remainingBudget,
            boolean isBudgetExceeded
    ) {}
}