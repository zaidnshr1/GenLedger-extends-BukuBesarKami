package com.bukubesarkami.features.adminpusat.controller;

import com.bukubesarkami.common.util.ApiResponse;
import com.bukubesarkami.common.util.SecurityUtil;
import com.bukubesarkami.features.adminpusat.dto.AdminPusatDto;
import com.bukubesarkami.features.adminpusat.service.ProjectManagementService;
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
@RequestMapping("/api/v1/admin-pusat/projects")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN_PUSAT')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin Pusat - Projects", description = "Manajemen proyek")
public class ProjectManagementController {

    private final ProjectManagementService projectManagementService;
    private final SecurityUtil securityUtil;

    @PostMapping
    @Operation(summary = "Buat proyek baru sekaligus assign Admin Project")
    public ResponseEntity<ApiResponse<AdminPusatDto.ProjectResponse>> createProject(
            @Valid @RequestBody AdminPusatDto.CreateProjectRequest request) {

        var result = projectManagementService.createProject(request, securityUtil.getCurrentUser());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Proyek berhasil dibuat", result));
    }

    @GetMapping
    @Operation(summary = "Daftar semua proyek")
    public ResponseEntity<ApiResponse<Page<AdminPusatDto.ProjectResponse>>> getAllProjects(@ParameterObject Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(projectManagementService.getAllProjects(pageable)));
    }

    @GetMapping("/{projectId}")
    @Operation(summary = "Detail proyek")
    public ResponseEntity<ApiResponse<AdminPusatDto.ProjectResponse>> getProject(@PathVariable UUID projectId) {
        return ResponseEntity.ok(ApiResponse.ok(projectManagementService.getProjectById(projectId)));
    }

    @PutMapping("/{projectId}")
    @Operation(summary = "Update proyek")
    public ResponseEntity<ApiResponse<AdminPusatDto.ProjectResponse>> updateProject(
            @PathVariable UUID projectId,
            @Valid @RequestBody AdminPusatDto.UpdateProjectRequest request) {

        return ResponseEntity.ok(ApiResponse.ok("Proyek diperbarui", projectManagementService.updateProject(projectId, request)));
    }

    @PostMapping("/{projectId}/assign-admin")
    @Operation(summary = "Tambah admin ke proyek")
    public ResponseEntity<ApiResponse<Void>> assignAdmin(
            @PathVariable UUID projectId,
            @Valid @RequestBody AdminPusatDto.AssignAdminRequest request) {

        projectManagementService.assignAdminToProject(projectId, request, securityUtil.getCurrentUser());
        return ResponseEntity.ok(ApiResponse.ok("Admin berhasil di-assign ke proyek"));
    }

    @DeleteMapping("/{projectId}/remove-admin/{userId}")
    @Operation(summary = "Hapus admin dari proyek")
    public ResponseEntity<ApiResponse<Void>> removeAdmin(
            @PathVariable UUID projectId,
            @PathVariable UUID userId) {

        projectManagementService.removeAdminFromProject(projectId, userId);
        return ResponseEntity.ok(ApiResponse.ok("Admin dihapus dari proyek"));
    }

    @PatchMapping("/{projectId}/toggle-status")
    @Operation(summary = "Aktifkan/nonaktifkan proyek")
    public ResponseEntity<ApiResponse<Void>> toggleStatus(@PathVariable UUID projectId) {
        projectManagementService.toggleProjectStatus(projectId);
        return ResponseEntity.ok(ApiResponse.ok("Status proyek diperbarui"));
    }
}