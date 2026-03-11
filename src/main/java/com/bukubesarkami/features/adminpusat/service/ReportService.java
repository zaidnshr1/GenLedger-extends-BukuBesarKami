package com.bukubesarkami.features.adminpusat.service;

import com.bukubesarkami.core.entity.Account;
import com.bukubesarkami.core.entity.JournalEntry;
import com.bukubesarkami.core.entity.JournalLine;
import com.bukubesarkami.core.entity.Project;
import com.bukubesarkami.core.repository.AccountRepository;
import com.bukubesarkami.core.repository.JournalEntryRepository;
import com.bukubesarkami.core.repository.ProjectRepository;
import com.bukubesarkami.features.adminpusat.dto.AdminPusatDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final ProjectRepository projectRepository;
    private final JournalEntryRepository journalEntryRepository;
    private final AccountRepository accountRepository;

    @Transactional(readOnly = true)
    public List<AdminPusatDto.ProfitLossResponse> getProfitLossAllProjects() {
        return projectRepository.findAll().stream()
                .map(this::buildProfitLoss)
                .toList();
    }

    @Transactional(readOnly = true)
    public AdminPusatDto.ProfitLossResponse getProfitLossByProject(UUID projectId) {
        Project project = projectRepository.findById(projectId).orElseThrow();
        return buildProfitLoss(project);
    }

    @Transactional(readOnly = true)
    public AdminPusatDto.TrialBalanceResponse getTrialBalance(UUID projectId) {
        Project project = projectRepository.findById(projectId).orElseThrow();

        Map<UUID, BigDecimal[]> balances = new LinkedHashMap<>();
        Map<UUID, Account> accountMap = new LinkedHashMap<>();

        List<JournalEntry> entries = journalEntryRepository
                .findAllByProjectId(projectId, org.springframework.data.domain.Pageable.unpaged())
                .stream()
                .filter(e -> e.getStatus() == JournalEntry.Status.POSTED)
                .toList();

        for (JournalEntry entry : entries) {
            for (JournalLine line : entry.getLines()) {
                UUID accId = line.getAccount().getId();
                accountMap.put(accId, line.getAccount());
                BigDecimal[] bal = balances.computeIfAbsent(accId, k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
                bal[0] = bal[0].add(line.getDebitAmount());
                bal[1] = bal[1].add(line.getCreditAmount());
            }
        }

        List<AdminPusatDto.TrialBalanceRow> rows = balances.entrySet().stream()
                .map(e -> {
                    Account acc = accountMap.get(e.getKey());
                    return new AdminPusatDto.TrialBalanceRow(
                            acc.getAccountCode(), acc.getAccountName(), acc.getAccountType().name(),
                            e.getValue()[0], e.getValue()[1]
                    );
                })
                .sorted(Comparator.comparing(AdminPusatDto.TrialBalanceRow::accountCode))
                .toList();

        BigDecimal totalDebit  = rows.stream().map(AdminPusatDto.TrialBalanceRow::debitBalance).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCredit = rows.stream().map(AdminPusatDto.TrialBalanceRow::creditBalance).reduce(BigDecimal.ZERO, BigDecimal::add);

        return new AdminPusatDto.TrialBalanceResponse(
                project.getId(), project.getProjectName(), rows, totalDebit, totalCredit
        );
    }

    // ===== PRIVATE =====

    private AdminPusatDto.ProfitLossResponse buildProfitLoss(Project project) {
        BigDecimal revenue = journalEntryRepository.sumRevenueByProject(project.getId());
        BigDecimal expense = journalEntryRepository.sumExpenseByProject(project.getId());
        BigDecimal net     = revenue.subtract(expense);

        return new AdminPusatDto.ProfitLossResponse(
                project.getId(), project.getProjectName(), revenue, expense, net
        );
    }
}