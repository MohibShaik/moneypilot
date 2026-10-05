package com.moneypilot.expense;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, UUID> {

    /** Finds an expense only if it is owned by the given user. */
    Optional<Expense> findByIdAndUserId(UUID id, UUID userId);

    /**
     * The user's expenses, newest expense date first, with optional inclusive
     * date range and category filter. A null filter is ignored.
     */
    @Query("""
            SELECT e FROM Expense e
            WHERE e.userId = :userId
              AND (:from IS NULL OR e.expenseDate >= :from)
              AND (:to IS NULL OR e.expenseDate <= :to)
              AND (:categoryId IS NULL OR e.categoryId = :categoryId)
            ORDER BY e.expenseDate DESC, e.createdAt DESC
            """)
    List<Expense> findForUserFiltered(
            @Param("userId") UUID userId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("categoryId") UUID categoryId);

    // --- Dashboard aggregates (inclusive date range) ---

    @Query("""
            SELECT COALESCE(SUM(e.amount), 0) FROM Expense e
            WHERE e.userId = :userId AND e.expenseDate BETWEEN :from AND :to
            """)
    BigDecimal sumForUserBetween(
            @Param("userId") UUID userId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    @Query("""
            SELECT COUNT(e) FROM Expense e
            WHERE e.userId = :userId AND e.expenseDate BETWEEN :from AND :to
            """)
    long countForUserBetween(
            @Param("userId") UUID userId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);
}
