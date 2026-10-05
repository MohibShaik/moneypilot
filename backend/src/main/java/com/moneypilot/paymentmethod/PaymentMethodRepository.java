package com.moneypilot.paymentmethod;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, UUID> {

    /** The user's payment methods, ordered by label. */
    List<PaymentMethod> findByUserIdOrderByLabelAsc(UUID userId);

    /** Finds a payment method only if it is owned by the given user. */
    Optional<PaymentMethod> findByIdAndUserId(UUID id, UUID userId);
}
