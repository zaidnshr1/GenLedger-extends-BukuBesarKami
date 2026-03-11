package com.bukubesarkami.features.adminproject.service;

import com.bukubesarkami.common.exception.AppException;
import com.bukubesarkami.common.util.SecurityUtil;
import com.bukubesarkami.core.entity.User;
import com.bukubesarkami.core.repository.JournalEntryRepository;
import com.bukubesarkami.core.repository.UserProjectRepository;
import com.bukubesarkami.features.adminproject.dto.AdminProjectDto;
import com.bukubesarkami.features.adminpusat.service.ProjectManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BudgetService {

    private final JournalEntryRepository journalEntryRepository;
    private final UserProjectRepository userProjectRepository;
    private final ProjectManagementService projectManagementService;
    private final SecurityUtil securityUtil;

    @Transactional(readOnly = true)
    public AdminProjectDto.BudgetSummaryResponse getBudgetSummary(UUID projectId) {
        User currentUser = securityUtil.getCurrentUser();

        // Role-based access: Admin Project hanya bisa lihat proyeknya sendiri
        if (currentUser.getRole() == User.Role.ADMIN_PROJECT) {
            boolean hasAccess = userProjectRepository.existsByUserIdAndProjectId(currentUser.getId(), projectId);
            if (!hasAccess) {
                throw new AppException.AccessDeniedException("Anda tidak memiliki akses ke proyek ini.");
            }
        }

        var project = projectManagementService.findProject(projectId);
        BigDecimal totalExpense = journalEntryRepository.sumExpenseByProject(projectId);
        BigDecimal totalRevenue = journalEntryRepository.sumRevenueByProject(projectId);
        BigDecimal remaining    = project.getBudget().subtract(totalExpense);

        return new AdminProjectDto.BudgetSummaryResponse(
                project.getId(), project.getProjectName(),
                project.getBudget(), totalExpense, totalRevenue,
                remaining, remaining.compareTo(BigDecimal.ZERO) < 0
        );
    }
}