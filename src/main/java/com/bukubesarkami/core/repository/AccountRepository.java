package com.bukubesarkami.core.repository;

import com.bukubesarkami.core.entity.Account;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccountRepository extends JpaRepository<Account, UUID> {

    boolean existsByAccountCode(String accountCode);

    Optional<Account> findByAccountCodeAndActiveTrue(String accountCode);

    // Eager load project untuk menghindari N+1
    @EntityGraph(attributePaths = {"project"})
    @Query("""
        SELECT a FROM Account a
        WHERE a.active = true
          AND (a.project IS NULL OR a.project.id = :projectId)
        """)
    Page<Account> findAvailableForProject(@Param("projectId") UUID projectId, Pageable pageable);

    // Akun global (kantor pusat) — List kecil, aman untuk cache
    @Query("SELECT a FROM Account a WHERE a.active = true AND a.project IS NULL ORDER BY a.accountCode")
    List<Account> findAllGlobalAccounts();

    // Paginated global accounts
    @Query(value = "SELECT a FROM Account a WHERE a.active = true AND a.project IS NULL",
            countQuery = "SELECT COUNT(a) FROM Account a WHERE a.active = true AND a.project IS NULL")
    Page<Account> findAllGlobalAccountsPaged(Pageable pageable);

    // Pessimistic lock — untuk operasi update saldo di masa depan
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.id = :id")
    Optional<Account> findByIdWithLock(@Param("id") UUID id);
}