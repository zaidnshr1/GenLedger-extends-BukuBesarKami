package com.bukubesarkami.features.adminproject.service;

import com.bukubesarkami.common.exception.AppException;
import com.bukubesarkami.core.entity.Account;
import com.bukubesarkami.core.entity.JournalEntry;
import com.bukubesarkami.core.entity.JournalLine;
import com.bukubesarkami.core.entity.User;
import com.bukubesarkami.core.repository.UserProjectRepository;
import com.bukubesarkami.features.adminproject.dto.AdminProjectDto;
import com.bukubesarkami.features.adminpusat.service.AccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JournalValidatorTest {

    @Mock
    private UserProjectRepository userProjectRepository;

    @Mock
    private AccountService accountService;

    @InjectMocks
    private JournalValidator journalValidator;

    private UUID projectId;
    private User adminProjectUser;

    @BeforeEach
    void setUp() {
        projectId = UUID.randomUUID();
        adminProjectUser = new User();
        adminProjectUser.setId(UUID.randomUUID());
        adminProjectUser.setRole(User.Role.ADMIN_PROJECT);
    }

    @Nested
    @DisplayName("Uji Validasi Akses Proyek")
    class AccessValidationTests {

        @Test
        @DisplayName("Harus lolos jika user adalah ADMIN_PUSAT")
        void shouldPassIfUserIsAdminPusat() {
            User adminPusat = new User();
            adminPusat.setRole(User.Role.ADMIN_PUSAT);

            assertDoesNotThrow(() -> journalValidator.validateProjectAccess(adminPusat, projectId));
            verifyNoInteractions(userProjectRepository);
        }

        @Test
        @DisplayName("Harus lempar exception jika user tidak punya akses ke proyek")
        void shouldThrowExceptionIfNoAccess() {
            when(userProjectRepository.existsByUserIdAndProjectId(adminProjectUser.getId(), projectId))
                    .thenReturn(false);

            assertThrows(AppException.AccessDeniedException.class,
                    () -> journalValidator.validateProjectAccess(adminProjectUser, projectId));
        }
    }

    @Nested
    @DisplayName("Uji Validasi Status & Double Entry")
    class BusinessRuleTests {

        @Test
        @DisplayName("Harus lempar exception jika status jurnal bukan DRAFT")
        void shouldThrowExceptionIfStatusNotDraft() {
            JournalEntry entry = new JournalEntry();
            entry.setStatus(JournalEntry.Status.POSTED);

            assertThrows(AppException.BusinessException.class,
                    () -> journalValidator.validateDraftStatus(entry));
        }

        @Test
        @DisplayName("Harus lempar exception jika debit tidak sama dengan kredit")
        void shouldThrowExceptionIfUnbalanced() {
            BigDecimal debit = new BigDecimal("1000");
            BigDecimal credit = new BigDecimal("900");

            assertThrows(AppException.UnbalancedEntryException.class,
                    () -> journalValidator.validateDoubleEntry(debit, credit));
        }

        @Test
        @DisplayName("Harus lolos jika debit sama dengan kredit")
        void shouldPassIfBalanced() {
            BigDecimal amount = new BigDecimal("1000");
            assertDoesNotThrow(() -> journalValidator.validateDoubleEntry(amount, amount));
        }
    }

    @Nested
    @DisplayName("Uji Pembuatan Baris Jurnal (buildLines)")
    class BuildLinesTests {

        @Test
        @DisplayName("Harus berhasil membuat lines jika request valid")
        void shouldBuildLinesSuccessfully() {
            UUID accountId = UUID.randomUUID();
            Account mockAccount = new Account();

            AdminProjectDto.JournalLineRequest req1 = new AdminProjectDto.JournalLineRequest(
                    accountId, new BigDecimal("100"), BigDecimal.ZERO, "Debit desc"
            );
            AdminProjectDto.JournalLineRequest req2 = new AdminProjectDto.JournalLineRequest(
                    accountId, BigDecimal.ZERO, new BigDecimal("100"), "Credit desc"
            );

            when(accountService.findActive(accountId)).thenReturn(mockAccount);

            List<JournalLine> result = journalValidator.buildLines(Arrays.asList(req1, req2));

            assertEquals(2, result.size());
            assertEquals(new BigDecimal("100"), result.get(0).getDebitAmount());
            assertEquals(new BigDecimal("100"), result.get(1).getCreditAmount());
        }

        @Test
        @DisplayName("Harus lempar exception jika debit dan kredit diisi sekaligus")
        void shouldThrowIfBothDebitAndCreditFilled() {
            AdminProjectDto.JournalLineRequest invalidReq = new AdminProjectDto.JournalLineRequest(
                    UUID.randomUUID(), new BigDecimal("100"), new BigDecimal("100"), "Invalid"
            );

            assertThrows(AppException.BusinessException.class,
                    () -> journalValidator.buildLines(List.of(invalidReq)));
        }
    }

    @Test
    @DisplayName("Uji Kalkulasi Total")
    void testCalculateTotals() {
        JournalLine line1 = JournalLine.builder()
                .debitAmount(new BigDecimal("500")).creditAmount(BigDecimal.ZERO).build();
        JournalLine line2 = JournalLine.builder()
                .debitAmount(BigDecimal.ZERO).creditAmount(new BigDecimal("500")).build();

        BigDecimal[] totals = journalValidator.calculateTotals(Arrays.asList(line1, line2));

        assertEquals(0, totals[0].compareTo(new BigDecimal("500")));
        assertEquals(0, totals[1].compareTo(new BigDecimal("500")));
    }
}