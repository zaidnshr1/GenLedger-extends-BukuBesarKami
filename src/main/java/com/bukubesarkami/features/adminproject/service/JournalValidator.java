package com.bukubesarkami.features.adminproject.service;

import com.bukubesarkami.common.exception.AppException;
import com.bukubesarkami.core.entity.Account;
import com.bukubesarkami.core.entity.JournalEntry;
import com.bukubesarkami.core.entity.JournalLine;
import com.bukubesarkami.core.entity.User;
import com.bukubesarkami.core.repository.UserProjectRepository;
import com.bukubesarkami.features.adminproject.dto.AdminProjectDto;
import com.bukubesarkami.features.adminpusat.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JournalValidator {

    private final UserProjectRepository userProjectRepository;
    private final AccountService accountService;

    public void validateProjectAccess(User user, UUID projectId) {
        if (user.getRole() == User.Role.ADMIN_PUSAT) return;
        boolean hasAccess = userProjectRepository.existsByUserIdAndProjectId(user.getId(), projectId);
        if (!hasAccess) {
            throw new AppException.AccessDeniedException("Anda tidak memiliki akses ke proyek ini.");
        }
    }

    public void validateDraftStatus(JournalEntry entry) {
        if (entry.getStatus() != JournalEntry.Status.DRAFT) {
            throw new AppException.BusinessException(
                    "Hanya jurnal berstatus DRAFT yang dapat diubah. Status saat ini: " + entry.getStatus()
            );
        }
    }

    public void validateDoubleEntry(BigDecimal debit, BigDecimal credit) {
        if (debit.compareTo(BigDecimal.ZERO) <= 0 || debit.compareTo(credit) != 0) {
            throw new AppException.UnbalancedEntryException();
        }
    }

    public void validateMinimumLines(List<JournalLine> lines) {
        if (lines.size() < 2) {
            throw new AppException.BusinessException("Jurnal minimal memiliki 2 baris.");
        }
    }

    public List<JournalLine> buildLines(List<AdminProjectDto.JournalLineRequest> requests) {
        List<JournalLine> lines = new ArrayList<>();
        for (int i = 0; i < requests.size(); i++) {
            AdminProjectDto.JournalLineRequest req = requests.get(i);
            Account account = accountService.findActive(req.accountId());

            boolean hasDebit  = req.debitAmount().compareTo(BigDecimal.ZERO) > 0;
            boolean hasCredit = req.creditAmount().compareTo(BigDecimal.ZERO) > 0;

            if (hasDebit && hasCredit) {
                throw new AppException.BusinessException(
                        "Baris #" + (i + 1) + ": tidak boleh mengisi debit dan kredit sekaligus."
                );
            }
            if (!hasDebit && !hasCredit) {
                throw new AppException.BusinessException(
                        "Baris #" + (i + 1) + ": harus mengisi salah satu antara debit atau kredit."
                );
            }

            lines.add(JournalLine.builder()
                    .account(account)
                    .debitAmount(req.debitAmount())
                    .creditAmount(req.creditAmount())
                    .description(req.description())
                    .lineOrder(i + 1)
                    .build());
        }
        return lines;
    }

    public BigDecimal[] calculateTotals(List<JournalLine> lines) {
        BigDecimal totalDebit  = lines.stream().map(JournalLine::getDebitAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCredit = lines.stream().map(JournalLine::getCreditAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new BigDecimal[]{totalDebit, totalCredit};
    }
}