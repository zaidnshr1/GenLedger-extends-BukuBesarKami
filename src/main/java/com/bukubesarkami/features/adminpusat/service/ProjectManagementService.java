package com.bukubesarkami.features.adminpusat.service;

import com.bukubesarkami.common.exception.AppException;
import com.bukubesarkami.core.entity.Project;
import com.bukubesarkami.core.entity.User;
import com.bukubesarkami.core.entity.UserProject;
import com.bukubesarkami.core.repository.ProjectRepository;
import com.bukubesarkami.core.repository.UserProjectRepository;
import com.bukubesarkami.features.adminpusat.dto.AdminPusatDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProjectManagementService {

    private final ProjectRepository projectRepository;
    private final UserProjectRepository userProjectRepository;
    private final UserManagementService userManagementService;

    @Transactional
    public AdminPusatDto.ProjectResponse createProject(AdminPusatDto.CreateProjectRequest request, User creator) {
        if (projectRepository.existsByProjectCode(request.projectCode().toUpperCase())) {
            throw new AppException.DuplicateException("Kode proyek sudah digunakan: " + request.projectCode());
        }

        User adminProject = userManagementService.findUser(request.adminProjectId());
        if (adminProject.getRole() != User.Role.ADMIN_PROJECT) {
            throw new AppException.BusinessException("User yang dipilih bukan Admin Project.");
        }

        Project project = Project.builder()
                .projectCode(request.projectCode().toUpperCase().trim())
                .projectName(request.projectName().trim())
                .description(request.description())
                .budget(request.budget())
                .active(true)
                .createdBy(creator)
                .build();

        project = projectRepository.save(project);

        // Assign admin project ke proyek
        assignAdmin(project, adminProject, creator);

        return toResponse(project);
    }

    @Transactional(readOnly = true)
    public Page<AdminPusatDto.ProjectResponse> getAllProjects(Pageable pageable) {
        return projectRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public AdminPusatDto.ProjectResponse getProjectById(UUID projectId) {
        return toResponse(findProject(projectId));
    }

    @Transactional
    public AdminPusatDto.ProjectResponse updateProject(UUID projectId, AdminPusatDto.UpdateProjectRequest request) {
        Project project = findProject(projectId);

        if (request.projectName() != null)  project.setProjectName(request.projectName().trim());
        if (request.description() != null)  project.setDescription(request.description());
        if (request.budget() != null)       project.setBudget(request.budget());

        return toResponse(projectRepository.save(project));
    }

    @Transactional
    public void assignAdminToProject(UUID projectId, AdminPusatDto.AssignAdminRequest request, User assigner) {
        Project project = findProject(projectId);
        User user = userManagementService.findUser(request.userId());

        if (user.getRole() != User.Role.ADMIN_PROJECT) {
            throw new AppException.BusinessException("Hanya Admin Project yang dapat di-assign ke proyek.");
        }
        if (userProjectRepository.existsByUserIdAndProjectId(user.getId(), projectId)) {
            throw new AppException.DuplicateException("User sudah ter-assign ke proyek ini.");
        }

        assignAdmin(project, user, assigner);
    }

    @Transactional
    public void removeAdminFromProject(UUID projectId, UUID userId) {
        UserProject up = userProjectRepository.findByUserIdAndProjectId(userId, projectId)
                .orElseThrow(() -> new AppException.NotFoundException("User tidak ter-assign ke proyek ini."));
        userProjectRepository.delete(up);
    }

    @Transactional
    public void toggleProjectStatus(UUID projectId) {
        Project project = findProject(projectId);
        project.setActive(!project.isActive());
        projectRepository.save(project);
    }

    // ===== HELPERS =====

    public Project findProject(UUID projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new AppException.NotFoundException("Proyek tidak ditemukan: " + projectId));
    }

    private void assignAdmin(Project project, User user, User assigner) {
        UserProject up = UserProject.builder()
                .user(user)
                .project(project)
                .assignedBy(assigner)
                .build();
        userProjectRepository.save(up);
    }

    public AdminPusatDto.ProjectResponse toResponse(Project project) {
        List<AdminPusatDto.UserResponse> admins = userProjectRepository
                .findAllByProjectId(project.getId()).stream()
                .map(up -> userManagementService.toResponse(up.getUser()))
                .toList();

        return new AdminPusatDto.ProjectResponse(
                project.getId(), project.getProjectCode(), project.getProjectName(),
                project.getDescription(), project.getBudget(), project.isActive(),
                admins, project.getCreatedAt(), project.getUpdatedAt()
        );
    }
}