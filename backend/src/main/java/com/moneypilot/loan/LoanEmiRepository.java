package com.moneypilot.loan;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface LoanEmiRepository extends JpaRepository<LoanEmi, UUID> {

    /** A loan's EMI schedule, ordered by installment number. */
    List<LoanEmi> findByLoanIdOrderByInstallmentNoAsc(UUID loanId);

    /**
     * Finds an EMI by id only if its parent loan is owned by the given user.
     * Used by the Payments module to validate a loan-EMI payment target.
     */
    @Query("""
            SELECT e FROM LoanEmi e
            WHERE e.id = :emiId
              AND e.loanId IN (SELECT l.id FROM Loan l WHERE l.id = e.loanId AND l.userId = :userId)
            """)
    Optional<LoanEmi> findOwnedByIdAndUserId(@Param("emiId") UUID emiId, @Param("userId") UUID userId);

    /**
     * Deletes all EMI rows for a loan. Used before deleting the parent loan,
     * since {@code loan_emis.loan_id} is ON DELETE RESTRICT.
     */
    @Modifying
    void deleteByLoanId(UUID loanId);

    /**
     * Unpaid EMIs across the user's loans (UPCOMING/DUE/OVERDUE), soonest due
     * first. Used by the dashboard's upcoming-dues list.
     */
    @Query("""
            SELECT e FROM LoanEmi e
            WHERE e.loanId IN (SELECT l.id FROM Loan l WHERE l.userId = :userId)
              AND e.status IN ('UPCOMING', 'DUE', 'OVERDUE')
            ORDER BY e.dueDate ASC
            """)
    List<LoanEmi> findUnpaidForUser(@Param("userId") UUID userId);
}
