package com.bukubesarkami.features.adminpusat.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public final class AdminPusatDto {

    private AdminPusatDto() {}

    // ===== USER MANAGEMENT =====

    public record CreateUserRequest(
            @NotBlank @Size(min = 3, max = 50) String username,
            @NotBlank @Email String email,
            @NotBlank
            @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&]).{8,}$",
                    message = "Password tidak memenuhi kriteria keamanan")
            String password,
            @NotBlank @Size(min = 2, max = 100) String fullName
    ) {}

    public record UserResponse(
            UUID id, String username, String email,
            String fullName, String role, boolean active,
            OffsetDateTime createdAt, OffsetDateTime updatedAt
    ) {}

    // ===== PROJECT MANAGEMENT =====

    public record CreateProjectRequest(
            @NotBlank @Size(min = 3, max = 20) String projectCode,
            @NotBlank @Size(min = 3, max = 100) String projectName,
            String description,
            @NotNull @DecimalMin("0") BigDecimal budget,
            @NotNull UUID adminProjectId
    ) {}

    public record UpdateProjectRequest(
            @Size(min = 3, max = 100) String projectName,
            String description,
            @DecimalMin("0") BigDecimal budget
    ) {}

    public record ProjectResponse(
            UUID id, String projectCode, String projectName,
            String description, BigDecimal budget, boolean active,
            List<UserResponse> admins,
            OffsetDateTime createdAt, OffsetDateTime updatedAt
    ) {}

    public record AssignAdminRequest(
            @NotNull UUID userId
    ) {}

    // ===== ACCOUNT (COA) =====

    public record CreateAccountRequest(
            @NotBlank @Size(max = 20) String accountCode,
            @NotBlank @Size(max = 100) String accountName,
            @NotBlank String accountType,
            @NotBlank String normalBalance,
            UUID projectId
    ) {}

    public record AccountResponse(
            UUID id, String accountCode, String accountName,
            String accountType, String normalBalance,
            UUID projectId, boolean active
    ) {}

    // ===== REPORTS =====

    public record ProfitLossResponse(
            UUID projectId, String projectName,
            BigDecimal totalRevenue, BigDecimal totalExpense, BigDecimal netProfit
    ) {}

    public record TrialBalanceRow(
            String accountCode, String accountName, String accountType,
            BigDecimal debitBalance, BigDecimal creditBalance
    ) {}

    public record TrialBalanceResponse(
            UUID projectId, String projectName,
            List<TrialBalanceRow> rows,
            BigDecimal totalDebit, BigDecimal totalCredit
    ) {}
}