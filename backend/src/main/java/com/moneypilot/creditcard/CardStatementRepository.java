package com.moneypilot.creditcard;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CardStatementRepository extends JpaRepository<CardStatement, UUID> {

    /** A card's statements, newest statement date first. */
    List<CardStatement> findByCreditCardIdOrderByStatementDateDesc(UUID creditCardId);

    /** Finds a statement only if it belongs to the given card. */
    Optional<CardStatement> findByIdAndCreditCardId(UUID id, UUID creditCardId);

    /**
     * Finds a statement by id only if its parent card is owned by the given
     * user. Used by the Payments module to validate a credit-card payment
     * target.
     */
    @Query("""
            SELECT s FROM CardStatement s
            WHERE s.id = :statementId
              AND s.creditCardId IN (
                  SELECT c.id FROM CreditCard c WHERE c.id = s.creditCardId AND c.userId = :userId)
            """)
    Optional<CardStatement> findOwnedByIdAndUserId(
            @Param("statementId") UUID statementId, @Param("userId") UUID userId);

    /**
     * Deletes all statements for a card. Used before deleting the parent card,
     * since {@code card_statements.credit_card_id} is ON DELETE RESTRICT.
     */
    @Modifying
    void deleteByCreditCardId(UUID creditCardId);

    // --- Dashboard aggregates ---

    /** Outstanding balance across the user's non-PAID statements. */
    @Query("""
            SELECT COALESCE(SUM(s.statementAmount - s.paidAmount), 0)
            FROM CardStatement s
            WHERE s.creditCardId IN (SELECT c.id FROM CreditCard c WHERE c.userId = :userId)
              AND s.status <> 'PAID'
            """)
    BigDecimal sumOutstandingForUser(@Param("userId") UUID userId);

    /** The user's unsettled (non-PAID) statements, soonest due first. */
    @Query("""
            SELECT s FROM CardStatement s
            WHERE s.creditCardId IN (SELECT c.id FROM CreditCard c WHERE c.userId = :userId)
              AND s.status <> 'PAID'
            ORDER BY s.dueDate ASC
            """)
    List<CardStatement> findUnsettledForUser(@Param("userId") UUID userId);
}
