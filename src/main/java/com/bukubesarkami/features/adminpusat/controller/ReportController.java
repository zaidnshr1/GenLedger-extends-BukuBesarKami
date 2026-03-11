package com.bukubesarkami.features.adminpusat.controller;

import com.bukubesarkami.common.util.ApiResponse;
import com.bukubesarkami.features.adminpusat.dto.AdminPusatDto;
import com.bukubesarkami.features.adminpusat.service.AuditLogService;
import com.bukubesarkami.features.adminpusat.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin-pusat")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN_PUSAT')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin Pusat - Reports & Audit", description = "Laporan keuangan dan audit log")
public class ReportController {

    private final ReportService reportService;
    private final AuditLogService auditLogService;

    @GetMapping("/reports/profit-loss")
    @Operation(summary = "Laporan P&L seluruh proyek")
    public ResponseEntity<ApiResponse<List<AdminPusatDto.ProfitLossResponse>>> profitLossAll() {
        return ResponseEntity.ok(ApiResponse.ok(reportService.getProfitLossAllProjects()));
    }

    @GetMapping("/reports/profit-loss/{projectId}")
    @Operation(summary = "Laporan P&L per proyek")
    public ResponseEntity<ApiResponse<AdminPusatDto.ProfitLossResponse>> profitLossByProject(
            @PathVariable UUID projectId) {

        return ResponseEntity.ok(ApiResponse.ok(reportService.getProfitLossByProject(projectId)));
    }

    @GetMapping("/reports/trial-balance/{projectId}")
    @Operation(summary = "Neraca saldo per proyek")
    public ResponseEntity<ApiResponse<AdminPusatDto.TrialBalanceResponse>> trialBalance(
            @PathVariable UUID projectId) {

        return ResponseEntity.ok(ApiResponse.ok(reportService.getTrialBalance(projectId)));
    }

    @GetMapping("/audit-logs")
    @Operation(summary = "Seluruh aktivitas sistem")
    public ResponseEntity<ApiResponse<Page<AuditLogService.AuditLogResponse>>> getAllAuditLogs(@ParameterObject Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(auditLogService.getAllLogs(pageable)));
    }

    @GetMapping("/audit-logs/project/{projectId}")
    @Operation(summary = "Aktivitas per proyek")
    public ResponseEntity<ApiResponse<Page<AuditLogService.AuditLogResponse>>> getProjectAuditLogs(
            @PathVariable UUID projectId, @ParameterObject Pageable pageable) {

        return ResponseEntity.ok(ApiResponse.ok(auditLogService.getLogsByProject(projectId, pageable)));
    }
}