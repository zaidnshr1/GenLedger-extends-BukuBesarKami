package com.bukubesarkami.features.adminpusat.controller;

import com.bukubesarkami.common.util.ApiResponse;
import com.bukubesarkami.common.util.SecurityUtil;
import com.bukubesarkami.features.adminpusat.dto.AdminPusatDto;
import com.bukubesarkami.features.adminpusat.service.UserManagementService;
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

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin-pusat/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN_PUSAT')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin Pusat - Users", description = "Manajemen user Admin Project")
public class UserManagementController {

    private final UserManagementService userManagementService;

    @PostMapping
    @Operation(summary = "Buat user Admin Project baru")
    public ResponseEntity<ApiResponse<AdminPusatDto.UserResponse>> createUser(
            @Valid @RequestBody AdminPusatDto.CreateUserRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("User berhasil dibuat", userManagementService.createAdminProject(request)));
    }

    @GetMapping
    @Operation(summary = "Daftar semua user")
    public ResponseEntity<ApiResponse<Page<AdminPusatDto.UserResponse>>> getAllUsers(@ParameterObject Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(userManagementService.getAllUsers(pageable)));
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Detail user")
    public ResponseEntity<ApiResponse<AdminPusatDto.UserResponse>> getUserById(@PathVariable UUID userId) {
        return ResponseEntity.ok(ApiResponse.ok(userManagementService.getUserById(userId)));
    }

    @PatchMapping("/{userId}/toggle-status")
    @Operation(summary = "Aktifkan/nonaktifkan user")
    public ResponseEntity<ApiResponse<AdminPusatDto.UserResponse>> toggleStatus(@PathVariable UUID userId) {
        return ResponseEntity.ok(ApiResponse.ok("Status user diperbarui", userManagementService.toggleUserStatus(userId)));
    }
}