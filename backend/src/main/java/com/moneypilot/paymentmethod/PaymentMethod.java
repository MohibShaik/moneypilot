package com.moneypilot.paymentmethod;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

/**
 * JPA entity mapped to the V2 {@code payment_methods} table.
 *
 * <p>Conventions match the other entities: {@code id}, {@code created_at}, and
 * {@code updated_at} are database-generated ({@code insertable=false} +
 * {@link Generated}) and read back after insert; Hibernate runs with
 * {@code ddl-auto=validate}.
 *
 * <p>{@code creditCardId} is set only when {@code type == CREDIT_CARD} and
 * references a card owned by the same user (validated by the service via
 * {@code CreditCardRepository.findByIdAndUserId}). For all other types it is
 * {@code null}. The DB CHECK {@code chk_payment_methods_credit_card_link}
 * remains the structural backstop.
 */
@Entity
@Table(name = "payment_methods")
public class PaymentMethod {

    @Id
    @Generated(event = EventType.INSERT)
    @Column(name = "id", columnDefinition = "uuid", nullable = false, updatable = false,
            insertable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private PaymentMethodType type;

    @Column(name = "label", nullable = false)
    private String label;

    /** Links to an owned credit card when type is CREDIT_CARD; otherwise null. */
    @Column(name = "credit_card_id")
    private UUID creditCardId;

    @Generated(event = EventType.INSERT)
    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Generated(event = {EventType.INSERT, EventType.UPDATE})
    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime updatedAt;

    protected PaymentMethod() {
        // Required by JPA.
    }

    /**
     * Creates a non-credit-card payment method (creditCardId stays null). The
     * generated columns are populated by the database after save.
     */
    public PaymentMethod(UUID userId, PaymentMethodType type, String label) {
        this(userId, type, label, null);
    }

    /**
     * Creates a payment method, optionally linking an owned credit card. The
     * caller is responsible for ensuring {@code creditCardId} is non-null only
     * when {@code type == CREDIT_CARD} and references a card owned by the user.
     */
    public PaymentMethod(UUID userId, PaymentMethodType type, String label, UUID creditCardId) {
        this.userId = userId;
        this.type = type;
        this.label = label;
        this.creditCardId = creditCardId;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public PaymentMethodType getType() {
        return type;
    }

    public void setType(PaymentMethodType type) {
        this.type = type;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public UUID getCreditCardId() {
        return creditCardId;
    }

    public void setCreditCardId(UUID creditCardId) {
        this.creditCardId = creditCardId;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
