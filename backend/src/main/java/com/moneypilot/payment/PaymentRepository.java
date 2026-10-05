package com.moneypilot.payment;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    /** Finds a payment only if it is owned by the given user. */
    Optional<Payment> findByIdAndUserId(UUID id, UUID userId);

    /**
     * The user's payments, newest {@code paid_at} first, with optional type and
     * inclusive paid-at range filters. A null filter is ignored.
     */
    @Query("""
            SELECT p FROM Payment p
            WHERE p.userId = :userId
              AND (:type IS NULL OR p.paymentType = :type)
              AND (:from IS NULL OR p.paidAt >= :from)
              AND (:to IS NULL OR p.paidAt <= :to)
            ORDER BY p.paidAt DESC, p.createdAt DESC
            """)
    List<Payment> findForUserFiltered(
            @Param("userId") UUID userId,
            @Param("type") PaymentType type,
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to);

    /** Total of all payments targeting a loan EMI (0 when none). */
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.loanEmiId = :emiId")
    BigDecimal sumByLoanEmiId(@Param("emiId") UUID emiId);

    /** Total of all payments targeting a card statement (0 when none). */
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.cardStatementId = :statementId")
    BigDecimal sumByCardStatementId(@Param("statementId") UUID statementId);
}
