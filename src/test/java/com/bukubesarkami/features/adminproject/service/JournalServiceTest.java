package com.bukubesarkami.features.adminproject.service;

import com.bukubesarkami.common.util.EntryNumberGenerator;
import com.bukubesarkami.common.util.IdempotencyService;
import com.bukubesarkami.common.util.SecurityUtil;
import com.bukubesarkami.core.entity.*;
import com.bukubesarkami.core.repository.JournalEntryRepository;
import com.bukubesarkami.features.adminproject.dto.AdminProjectDto;
import com.bukubesarkami.features.adminpusat.service.AuditLogService;
import com.bukubesarkami.features.adminpusat.service.ProjectManagementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JournalServiceTest {

    @Mock private JournalEntryRepository journalEntryRepository;
    @Mock private ProjectManagementService projectManagementService;
    @Mock private AuditLogService auditLogService;
    @Mock private EntryNumberGenerator entryNumberGenerator;
    @Mock private JournalValidator validator;
    @Mock private IdempotencyService idempotencyService;
    @Mock private JournalMapper mapper;
    @Mock private SecurityUtil securityUtil;

    @InjectMocks
    private JournalService journalService;

    private User mockUser;
    private Project mockProject;

    @BeforeEach
    void setUp() {
        mockUser = new User();
        mockUser.setId(UUID.randomUUID());

        mockProject = new Project();
        mockProject.setId(UUID.randomUUID());
    }

    @Test
    @DisplayName("Skenario Berhasil: Membuat Jurnal Baru")
    void createEntry_ShouldSucceed() {
        // Arrange
        AdminProjectDto.CreateJournalRequest request = new AdminProjectDto.CreateJournalRequest(
                mockProject.getId(), LocalDate.now(), "Test Journal", "REF-001", new ArrayList<>()
        );

        List<JournalLine> mockLines = List.of(new JournalLine());
        BigDecimal[] mockTotals = {new BigDecimal("1000"), new BigDecimal("1000")};

        when(securityUtil.getCurrentUser()).thenReturn(mockUser);
        when(projectManagementService.findProject(any())).thenReturn(mockProject);
        when(validator.buildLines(any())).thenReturn(mockLines);
        when(validator.calculateTotals(any())).thenReturn(mockTotals);
        when(entryNumberGenerator.generate(any())).thenReturn("JRN-001");
        when(journalEntryRepository.existsByEntryNumber(anyString())).thenReturn(false);
        when(journalEntryRepository.save(any(JournalEntry.class))).thenAnswer(i -> i.getArguments()[0]);

        journalService.createEntry(request, "key-123", "127.0.0.1");

        verify(idempotencyService).checkAndMark("key-123");
        verify(journalEntryRepository).save(any(JournalEntry.class));
        verify(idempotencyService).commit("key-123");
        verify(auditLogService).log(eq(mockUser), eq(mockProject), eq("CREATE"), anyString(), any(), any(), anyString(), anyString());
    }

    @Test
    @DisplayName("Skenario Berhasil: Posting Jurnal (Draft ke Posted)")
    void postEntry_ShouldSucceed() {
        UUID entryId = UUID.randomUUID();
        JournalEntry entry = new JournalEntry();
        entry.setProject(mockProject);
        entry.setStatus(JournalEntry.Status.DRAFT);
        entry.setTotalDebit(new BigDecimal("1000"));
        entry.setTotalCredit(new BigDecimal("1000"));

        when(securityUtil.getCurrentUser()).thenReturn(mockUser);
        when(journalEntryRepository.findByIdForUpdate(entryId)).thenReturn(java.util.Optional.of(entry));
        when(journalEntryRepository.save(any(JournalEntry.class))).thenReturn(entry);

        journalService.postEntry(entryId, "127.0.0.1");

        assertEquals(JournalEntry.Status.POSTED, entry.getStatus());
        assertEquals(mockUser, entry.getPostedBy());
        verify(journalEntryRepository).save(entry);
    }
}