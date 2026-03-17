package com.bukubesarkami.features.adminproject.service;

import com.bukubesarkami.common.util.SecurityUtil;
import com.bukubesarkami.core.entity.Project;
import com.bukubesarkami.core.entity.User;
import com.bukubesarkami.core.repository.JournalEntryRepository;
import com.bukubesarkami.core.repository.UserProjectRepository;
import com.bukubesarkami.features.adminproject.dto.AdminProjectDto;
import com.bukubesarkami.features.adminpusat.service.ProjectManagementService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BudgetServiceTest {

    @Mock private JournalEntryRepository journalEntryRepository;
    @Mock private UserProjectRepository userProjectRepository;
    @Mock private ProjectManagementService projectManagementService;
    @Mock private SecurityUtil securityUtil;

    @InjectMocks
    private BudgetService budgetService;

    @Test
    @DisplayName("Harus menghitung ringkasan anggaran dengan benar")
    void getBudgetSummary_ShouldCalculateCorrectly() {
        UUID projectId = UUID.randomUUID();
        User user = new User();
        user.setRole(User.Role.ADMIN_PUSAT); // Admin pusat bebas akses

        Project project = new Project();
        project.setId(projectId);
        project.setProjectName("Project Alpha");
        project.setBudget(new BigDecimal("5000"));

        when(securityUtil.getCurrentUser()).thenReturn(user);
        when(projectManagementService.findProject(projectId)).thenReturn(project);
        when(journalEntryRepository.sumExpenseByProject(projectId)).thenReturn(new BigDecimal("2000"));
        when(journalEntryRepository.sumRevenueByProject(projectId)).thenReturn(new BigDecimal("3000"));

        AdminProjectDto.BudgetSummaryResponse response = budgetService.getBudgetSummary(projectId);

        // Assert
        assertNotNull(response);
        assertEquals(new BigDecimal("5000"), response.totalBudget());
        assertEquals(new BigDecimal("2000"), response.totalExpense());
        assertEquals(new BigDecimal("3000"), response.totalRevenue());
        assertEquals(new BigDecimal("3000"), response.remainingBudget());
        assertFalse(response.isBudgetExceeded());
    }
}