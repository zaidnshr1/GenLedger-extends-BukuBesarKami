package com.bukubesarkami.features.adminpusat.service;

import com.bukubesarkami.common.exception.AppException;
import com.bukubesarkami.core.entity.Account;
import com.bukubesarkami.core.entity.Project;
import com.bukubesarkami.core.repository.AccountRepository;
import com.bukubesarkami.features.adminpusat.dto.AdminPusatDto;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final ProjectManagementService projectManagementService;

    @Transactional
    @CacheEvict(cacheNames = {"coa-global", "coa-project"}, allEntries = true)
    public AdminPusatDto.AccountResponse createAccount(AdminPusatDto.CreateAccountRequest request) {
        if (accountRepository.existsByAccountCode(request.accountCode().toUpperCase())) {
            throw new AppException.DuplicateException("Kode akun sudah ada: " + request.accountCode());
        }

        Account.AccountType accountType = parseAccountType(request.accountType());
        Account.NormalBalance normalBalance = parseNormalBalance(request.normalBalance());

        Project project = null;
        if (request.projectId() != null) {
            project = projectManagementService.findProject(request.projectId());
        }

        Account account = Account.builder()
                .accountCode(request.accountCode().toUpperCase().trim())
                .accountName(request.accountName().trim())
                .accountType(accountType)
                .normalBalance(normalBalance)
                .project(project)
                .active(true)
                .build();

        return toResponse(accountRepository.save(account));
    }

    /**
     * COA global jarang berubah tapi sangat sering diakses saat input jurnal.
     * Cache di Redis selama 1 jam — dikonfigurasi di RedisConfig.
     * Otomatis di-evict saat createAccount atau deactivateAccount dipanggil.
     */
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "coa-global")
    public List<AdminPusatDto.AccountResponse> getGlobalAccounts() {
        return accountRepository.findAllGlobalAccounts()
                .stream().map(this::toResponse).toList();
    }

    /**
     * COA per proyek di-cache dengan key composite (projectId + page).
     */
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "coa-project", key = "#projectId + ':' + #pageable.pageNumber + ':' + #pageable.pageSize")
    public Page<AdminPusatDto.AccountResponse> getAccountsForProject(UUID projectId, Pageable pageable) {
        return accountRepository.findAvailableForProject(projectId, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "coa-global", key = "'paged:' + #pageable.pageNumber + ':' + #pageable.pageSize")
    public Page<AdminPusatDto.AccountResponse> getGlobalAccountsPaged(Pageable pageable) {
        return accountRepository.findAllGlobalAccountsPaged(pageable).map(this::toResponse);
    }

    @Transactional
    @CacheEvict(cacheNames = {"coa-global", "coa-project"}, allEntries = true)
    public void deactivateAccount(UUID accountId) {
        Account account = findActiveById(accountId);
        account.setActive(false);
        accountRepository.save(account);
    }

    /** Digunakan oleh JournalValidator saat validasi line jurnal. */
    public Account findActive(UUID accountId) {
        return findActiveById(accountId);
    }

    public Account findActiveById(UUID accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AppException.NotFoundException("Akun tidak ditemukan: " + accountId));
        if (!account.isActive()) {
            throw new AppException.BusinessException("Akun tidak aktif: " + account.getAccountCode());
        }
        return account;
    }

    // ===== HELPERS =====

    private Account.AccountType parseAccountType(String type) {
        try {
            return Account.AccountType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new AppException.BusinessException("Tipe akun tidak valid: " + type);
        }
    }

    private Account.NormalBalance parseNormalBalance(String balance) {
        try {
            return Account.NormalBalance.valueOf(balance.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new AppException.BusinessException("Normal balance tidak valid: " + balance);
        }
    }

    public AdminPusatDto.AccountResponse toResponse(Account account) {
        return new AdminPusatDto.AccountResponse(
                account.getId(), account.getAccountCode(), account.getAccountName(),
                account.getAccountType().name(), account.getNormalBalance().name(),
                account.getProject() != null ? account.getProject().getId() : null,
                account.isActive()
        );
    }
}