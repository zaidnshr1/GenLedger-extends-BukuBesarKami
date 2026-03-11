package com.bukubesarkami.features.adminproject.service;

import com.bukubesarkami.core.entity.JournalEntry;
import com.bukubesarkami.features.adminproject.dto.AdminProjectDto;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class JournalMapper {

    public AdminProjectDto.JournalEntryResponse toResponse(JournalEntry entry) {
        List<AdminProjectDto.JournalLineResponse> lineResponses = entry.getLines().stream()
                .map(l -> new AdminProjectDto.JournalLineResponse(
                        l.getId(),
                        l.getAccount().getId(),
                        l.getAccount().getAccountCode(),
                        l.getAccount().getAccountName(),
                        l.getDebitAmount(),
                        l.getCreditAmount(),
                        l.getDescription(),
                        l.getLineOrder()
                ))
                .toList();

        return new AdminProjectDto.JournalEntryResponse(
                entry.getId(),
                entry.getEntryNumber(),
                entry.getProject() != null ? entry.getProject().getId() : null,
                entry.getEntryDate(),
                entry.getDescription(),
                entry.getReferenceNumber(),
                entry.getStatus().name(),
                entry.getTotalDebit(),
                entry.getTotalCredit(),
                lineResponses,
                entry.getCreatedBy().getId(),
                entry.getCreatedBy().getUsername(),
                entry.getCreatedAt(),
                entry.getUpdatedAt()
        );
    }
}