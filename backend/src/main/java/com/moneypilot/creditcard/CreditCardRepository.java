package com.moneypilot.creditcard;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CreditCardRepository extends JpaRepository<CreditCard, UUID> {

    /** The user's credit cards, newest first. */
    List<CreditCard> findByUserIdOrderByCreatedAtDesc(UUID userId);

    /**
     * Finds a credit card only if it is owned by the given user. Also used by
     * the Payment Methods module to validate card ownership before linking.
     */
    Optional<CreditCard> findByIdAndUserId(UUID id, UUID userId);

    // --- Dashboard aggregates (ACTIVE cards only) ---

    @Query("SELECT COUNT(c) FROM CreditCard c WHERE c.userId = :userId AND c.status = 'ACTIVE'")
    long countActiveByUserId(@Param("userId") UUID userId);

    @Query("""
            SELECT COALESCE(SUM(c.creditLimit), 0) FROM CreditCard c
            WHERE c.userId = :userId AND c.status = 'ACTIVE'
            """)
    BigDecimal sumCreditLimitActive(@Param("userId") UUID userId);
}
