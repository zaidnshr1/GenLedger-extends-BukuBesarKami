package com.bukubesarkami.features.adminproject.service;

import com.bukubesarkami.common.exception.AppException;
import com.bukubesarkami.common.util.EntryNumberGenerator;
import com.bukubesarkami.common.util.IdempotencyService;
import com.bukubesarkami.common.util.SecurityUtil;
import com.bukubesarkami.core.entity.*;
import com.bukubesarkami.core.repository.JournalEntryRepository;
import com.bukubesarkami.features.adminproject.dto.AdminProjectDto;
import com.bukubesarkami.features.adminpusat.service.AuditLogService;
import com.bukubesarkami.features.adminpusat.service.ProjectManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JournalService {

    private final JournalEntryRepository journalEntryRepository;
    private final ProjectManagementService projectManagementService;
    private final AuditLogService auditLogService;
    private final EntryNumberGenerator entryNumberGenerator;
    private final JournalValidator validator;
    private final IdempotencyService idempotencyService;
    private final JournalMapper mapper;
    private final SecurityUtil securityUtil;

    @Transactional
    public AdminProjectDto.JournalEntryResponse createEntry(AdminProjectDto.CreateJournalRequest request,
                                                            String idempotencyKey, String ipAddress) {
        idempotencyService.checkAndMark(idempotencyKey);
        try {
            User user = securityUtil.getCurrentUser();
            Project project = projectManagementService.findProject(request.projectId());
            validator.validateProjectAccess(user, project.getId());

            List<JournalLine> lines = validator.buildLines(request.lines());
            BigDecimal[] totals = validator.calculateTotals(lines);
            validator.validateDoubleEntry(totals[0], totals[1]);
            validator.validateMinimumLines(lines);

            String entryNumber = generateUniqueEntryNumber(request.entryDate());
            JournalEntry entry = JournalEntry.builder()
                    .entryNumber(entryNumber).project(project)
                    .entryDate(request.entryDate()).description(request.description().trim())
                    .referenceNumber(request.referenceNumber())
                    .status(JournalEntry.Status.DRAFT)
                    .totalDebit(totals[0]).totalCredit(totals[1])
                    .createdBy(user).lines(new ArrayList<>())
                    .build();

            lines.forEach(l -> l.setJournalEntry(entry));
            entry.getLines().addAll(lines);

            JournalEntry saved = journalEntryRepository.save(entry);
            idempotencyService.commit(idempotencyKey);
            auditLogService.log(user, project, "CREATE", "JOURNAL_ENTRY", saved.getId(), null, entryNumber, ipAddress);
            return mapper.toResponse(saved);

        } catch (AppException.DuplicateException e) {
            throw e;
        } catch (Exception e) {
            idempotencyService.rollback(idempotencyKey);
            throw e;
        }
    }

    @Transactional
    public AdminProjectDto.JournalEntryResponse updateEntry(UUID entryId,
                                                            AdminProjectDto.UpdateJournalRequest request,
                                                            String ipAddress) {
        User user = securityUtil.getCurrentUser();
        JournalEntry entry = findWithLock(entryId);
        validator.validateProjectAccess(user, entry.getProject().getId());
        validator.validateDraftStatus(entry);

        if (request.entryDate() != null)       entry.setEntryDate(request.entryDate());
        if (request.description() != null)     entry.setDescription(request.description().trim());
        if (request.referenceNumber() != null) entry.setReferenceNumber(request.referenceNumber());

        if (request.lines() != null && !request.lines().isEmpty()) {
            List<JournalLine> newLines = validator.buildLines(request.lines());
            BigDecimal[] totals = validator.calculateTotals(newLines);
            validator.validateDoubleEntry(totals[0], totals[1]);
            validator.validateMinimumLines(newLines);
            entry.getLines().clear();
            newLines.forEach(l -> l.setJournalEntry(entry));
            entry.getLines().addAll(newLines);
            entry.setTotalDebit(totals[0]);
            entry.setTotalCredit(totals[1]);
        }

        JournalEntry saved = journalEntryRepository.save(entry);
        auditLogService.log(user, entry.getProject(), "UPDATE", "JOURNAL_ENTRY", entryId, null, "updated", ipAddress);
        return mapper.toResponse(saved);
    }

    @Transactional
    public AdminProjectDto.JournalEntryResponse postEntry(UUID entryId, String ipAddress) {
        User user = securityUtil.getCurrentUser();
        JournalEntry entry = findWithLock(entryId);
        validator.validateProjectAccess(user, entry.getProject().getId());
        validator.validateDraftStatus(entry);
        validator.validateDoubleEntry(entry.getTotalDebit(), entry.getTotalCredit());

        entry.setStatus(JournalEntry.Status.POSTED);
        entry.setPostedBy(user);
        entry.setPostedAt(OffsetDateTime.now());

        JournalEntry saved = journalEntryRepository.save(entry);
        auditLogService.log(user, entry.getProject(), "POST", "JOURNAL_ENTRY", entryId, "DRAFT", "POSTED", ipAddress);
        return mapper.toResponse(saved);
    }

    @Transactional
    public AdminProjectDto.JournalEntryResponse voidEntry(UUID entryId,
                                                          AdminProjectDto.VoidJournalRequest request,
                                                          String ipAddress) {
        User user = securityUtil.getCurrentUser();
        JournalEntry entry = findWithLock(entryId);
        validator.validateProjectAccess(user, entry.getProject().getId());

        if (entry.getStatus() == JournalEntry.Status.VOIDED) {
            throw new AppException.BusinessException("Jurnal sudah dibatalkan.");
        }
        entry.setStatus(JournalEntry.Status.VOIDED);
        entry.setVoidedBy(user);
        entry.setVoidedAt(OffsetDateTime.now());
        entry.setVoidReason(request.reason().trim());

        JournalEntry saved = journalEntryRepository.save(entry);
        auditLogService.log(user, entry.getProject(), "VOID", "JOURNAL_ENTRY",
                entryId, "POSTED", "VOIDED: " + request.reason(), ipAddress);
        return mapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<AdminProjectDto.JournalEntryResponse> getEntriesByProject(UUID projectId, Pageable pageable) {
        validator.validateProjectAccess(securityUtil.getCurrentUser(), projectId);
        return journalEntryRepository.findAllByProjectId(projectId, pageable).map(mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public AdminProjectDto.JournalEntryResponse getEntryById(UUID entryId) {
        JournalEntry entry = journalEntryRepository.findByIdWithLines(entryId)
                .orElseThrow(() -> new AppException.NotFoundException("Jurnal tidak ditemukan: " + entryId));
        validator.validateProjectAccess(securityUtil.getCurrentUser(), entry.getProject().getId());
        return mapper.toResponse(entry);
    }

    // ===== PRIVATE =====

    private JournalEntry findWithLock(UUID entryId) {
        return journalEntryRepository.findByIdForUpdate(entryId)
                .orElseThrow(() -> new AppException.NotFoundException("Jurnal tidak ditemukan: " + entryId));
    }

    private String generateUniqueEntryNumber(java.time.LocalDate date) {
        String number;
        int attempt = 0;
        do {
            number = entryNumberGenerator.generate(date);
            if (attempt++ > 10) throw new AppException.BusinessException("Gagal generate nomor jurnal.");
        } while (journalEntryRepository.existsByEntryNumber(number));
        return number;
    }
}