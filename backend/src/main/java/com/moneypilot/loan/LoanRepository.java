package com.moneypilot.loan;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface LoanRepository extends JpaRepository<Loan, UUID> {

    /** The user's loans, newest first. */
    List<Loan> findByUserIdOrderByCreatedAtDesc(UUID userId);

    /** Finds a loan only if it is owned by the given user. */
    Optional<Loan> findByIdAndUserId(UUID id, UUID userId);

    // --- Dashboard aggregates (ACTIVE loans only) ---

    @Query("SELECT COUNT(l) FROM Loan l WHERE l.userId = :userId AND l.status = 'ACTIVE'")
    long countActiveByUserId(@Param("userId") UUID userId);

    @Query("""
            SELECT COALESCE(SUM(l.outstandingPrincipal), 0) FROM Loan l
            WHERE l.userId = :userId AND l.status = 'ACTIVE'
            """)
    BigDecimal sumOutstandingPrincipalActive(@Param("userId") UUID userId);

    @Query("""
            SELECT COALESCE(SUM(l.emiAmount), 0) FROM Loan l
            WHERE l.userId = :userId AND l.status = 'ACTIVE'
            """)
    BigDecimal sumMonthlyEmiActive(@Param("userId") UUID userId);
}
