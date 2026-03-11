package com.bukubesarkami.core.repository;

import com.bukubesarkami.core.entity.JournalEntry;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JournalEntryRepository extends JpaRepository<JournalEntry, UUID> {

    // Eager load createdBy untuk menghindari N+1 di list view
    @EntityGraph(attributePaths = {"createdBy", "project"})
    Page<JournalEntry> findAllByProjectId(UUID projectId, Pageable pageable);

    @EntityGraph(attributePaths = {"createdBy", "project"})
    Page<JournalEntry> findAllByProjectIdAndEntryDateBetween(
            UUID projectId, LocalDate startDate, LocalDate endDate, Pageable pageable
    );

    // Untuk detail — eager load semua relasi termasuk lines
    @EntityGraph(attributePaths = {"createdBy", "project", "lines", "lines.account"})
    @Query("SELECT je FROM JournalEntry je WHERE je.id = :id")
    Optional<JournalEntry> findByIdWithLines(@Param("id") UUID id);

    Optional<JournalEntry> findByEntryNumber(String entryNumber);

    boolean existsByEntryNumber(String entryNumber);

    // Pessimistic lock — cegah double spending saat POST jurnal bersamaan
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT je FROM JournalEntry je WHERE je.id = :id")
    Optional<JournalEntry> findByIdForUpdate(@Param("id") UUID id);

    @Query("""
        SELECT COALESCE(SUM(jl.debitAmount), 0)
        FROM JournalLine jl
        JOIN jl.journalEntry je
        WHERE je.project.id = :projectId
          AND je.status = 'POSTED'
          AND jl.account.accountType = 'EXPENSE'
        """)
    BigDecimal sumExpenseByProject(@Param("projectId") UUID projectId);

    @Query("""
        SELECT COALESCE(SUM(jl.creditAmount), 0)
        FROM JournalLine jl
        JOIN jl.journalEntry je
        WHERE je.project.id = :projectId
          AND je.status = 'POSTED'
          AND jl.account.accountType = 'REVENUE'
        """)
    BigDecimal sumRevenueByProject(@Param("projectId") UUID projectId);
}