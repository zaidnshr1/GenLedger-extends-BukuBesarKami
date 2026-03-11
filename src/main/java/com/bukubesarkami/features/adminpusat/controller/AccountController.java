package com.bukubesarkami.features.adminpusat.controller;

import com.bukubesarkami.common.util.ApiResponse;
import com.bukubesarkami.features.adminpusat.dto.AdminPusatDto;
import com.bukubesarkami.features.adminpusat.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin-pusat/accounts")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN_PUSAT')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin Pusat - Accounts", description = "Manajemen Chart of Accounts (COA) — hasil di-cache Redis")
public class AccountController {

    private final AccountService accountService;

    @PostMapping
    @Operation(summary = "Buat akun baru (cache COA akan di-evict)")
    public ResponseEntity<ApiResponse<AdminPusatDto.AccountResponse>> createAccount(
            @Valid @RequestBody AdminPusatDto.CreateAccountRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Akun berhasil dibuat", accountService.createAccount(request)));
    }

    @GetMapping("/global")
    @Operation(summary = "Daftar akun global — cached Redis 1 jam")
    public ResponseEntity<ApiResponse<List<AdminPusatDto.AccountResponse>>> getGlobalAccounts() {
        return ResponseEntity.ok(ApiResponse.ok(accountService.getGlobalAccounts()));
    }

    @GetMapping("/global/paged")
    @Operation(summary = "Daftar akun global dengan pagination")
    public ResponseEntity<ApiResponse<Page<AdminPusatDto.AccountResponse>>> getGlobalAccountsPaged(
            @ParameterObject Pageable pageable) {

        return ResponseEntity.ok(ApiResponse.ok(accountService.getGlobalAccountsPaged(pageable)));
    }

    @GetMapping("/project/{projectId}")
    @Operation(summary = "Daftar akun tersedia untuk proyek (paginated, cached)")
    public ResponseEntity<ApiResponse<Page<AdminPusatDto.AccountResponse>>> getProjectAccounts(
            @PathVariable UUID projectId, @ParameterObject Pageable pageable) {

        return ResponseEntity.ok(ApiResponse.ok(accountService.getAccountsForProject(projectId, pageable)));
    }

    @DeleteMapping("/{accountId}")
    @Operation(summary = "Nonaktifkan akun (cache COA akan di-evict)")
    public ResponseEntity<ApiResponse<Void>> deactivateAccount(@PathVariable UUID accountId) {
        accountService.deactivateAccount(accountId);
        return ResponseEntity.ok(ApiResponse.ok("Akun dinonaktifkan"));
    }
}