package com.bukubesarkami.features.adminproject.controller;

import com.bukubesarkami.common.util.ApiResponse;
import com.bukubesarkami.features.adminproject.dto.AdminProjectDto;
import com.bukubesarkami.features.adminproject.service.BudgetService;
import com.bukubesarkami.features.adminproject.service.JournalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/project")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN_PROJECT', 'ADMIN_PUSAT')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin Project - Journal", description = "Transaksi harian proyek (buku besar)")
public class JournalController {

    private final JournalService journalService;
    private final BudgetService budgetService;

    @PostMapping("/journals")
    @Operation(
            summary = "Buat entri jurnal baru (double-entry)",
            description = "Wajib menyertakan header **X-Idempotency-Key** (UUID unik per transaksi) " +
                    "untuk mencegah double submission. Key berlaku 24 jam."
    )
    public ResponseEntity<ApiResponse<AdminProjectDto.JournalEntryResponse>> createEntry(
            @Valid @RequestBody AdminProjectDto.CreateJournalRequest request,
            @Parameter(description = "UUID unik per transaksi, cegah double submit")
            @RequestHeader("X-Idempotency-Key") String idempotencyKey,
            HttpServletRequest httpRequest) {

        var result = journalService.createEntry(request, idempotencyKey, extractIp(httpRequest));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Jurnal berhasil dibuat", result));
    }

    @GetMapping("/journals/project/{projectId}")
    @Operation(summary = "Riwayat transaksi per proyek (paginated)")
    public ResponseEntity<ApiResponse<Page<AdminProjectDto.JournalEntryResponse>>> getByProject(
            @PathVariable UUID projectId, @ParameterObject Pageable pageable) {

        return ResponseEntity.ok(ApiResponse.ok(journalService.getEntriesByProject(projectId, pageable)));
    }

    @GetMapping("/journals/{entryId}")
    @Operation(summary = "Detail jurnal dengan semua baris")
    public ResponseEntity<ApiResponse<AdminProjectDto.JournalEntryResponse>> getById(
            @PathVariable UUID entryId) {

        return ResponseEntity.ok(ApiResponse.ok(journalService.getEntryById(entryId)));
    }

    @PutMapping("/journals/{entryId}")
    @Operation(summary = "Update jurnal (hanya status DRAFT)")
    public ResponseEntity<ApiResponse<AdminProjectDto.JournalEntryResponse>> updateEntry(
            @PathVariable UUID entryId,
            @Valid @RequestBody AdminProjectDto.UpdateJournalRequest request,
            HttpServletRequest httpRequest) {

        return ResponseEntity.ok(ApiResponse.ok("Jurnal diperbarui",
                journalService.updateEntry(entryId, request, extractIp(httpRequest))));
    }

    @PostMapping("/journals/{entryId}/post")
    @Operation(summary = "Post jurnal: DRAFT → POSTED (final, tidak bisa diubah)")
    public ResponseEntity<ApiResponse<AdminProjectDto.JournalEntryResponse>> postEntry(
            @PathVariable UUID entryId, HttpServletRequest httpRequest) {

        return ResponseEntity.ok(ApiResponse.ok("Jurnal berhasil diposting",
                journalService.postEntry(entryId, extractIp(httpRequest))));
    }

    @PostMapping("/journals/{entryId}/void")
    @Operation(summary = "Batalkan jurnal dengan alasan (tidak bisa undone)")
    public ResponseEntity<ApiResponse<AdminProjectDto.JournalEntryResponse>> voidEntry(
            @PathVariable UUID entryId,
            @Valid @RequestBody AdminProjectDto.VoidJournalRequest request,
            HttpServletRequest httpRequest) {

        return ResponseEntity.ok(ApiResponse.ok("Jurnal dibatalkan",
                journalService.voidEntry(entryId, request, extractIp(httpRequest))));
    }

    @GetMapping("/budget/{projectId}")
    @Operation(summary = "Cek saldo anggaran proyek")
    public ResponseEntity<ApiResponse<AdminProjectDto.BudgetSummaryResponse>> getBudget(
            @PathVariable UUID projectId) {

        return ResponseEntity.ok(ApiResponse.ok(budgetService.getBudgetSummary(projectId)));
    }

    private String extractIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) return xff.split(",")[0].trim();
        return request.getRemoteAddr();
    }
}