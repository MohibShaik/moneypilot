package com.moneypilot.payment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

/**
 * JPA entity mapped to the V2 {@code payments} table.
 *
 * <p>Conventions match the other entities: {@code id}, {@code created_at}, and
 * {@code updated_at} are database-generated ({@code insertable=false} +
 * {@link Generated}). Unlike the audit columns, {@code paid_at} is written by
 * the application (request value or now()), so it is a normal insertable
 * column.
 *
 * <p>The type/target shape ({@code chk_payments_target}) and target ownership
 * are enforced in the service before persisting; the target ids are modeled as
 * plain nullable UUID columns. {@code loan_emi_id}/{@code card_statement_id}
 * are intentionally non-unique so multiple partial payments can target the same
 * EMI or statement.
 */
@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @Generated(event = EventType.INSERT)
    @Column(name = "id", columnDefinition = "uuid", nullable = false, updatable = false,
            insertable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_type", nullable = false)
    private PaymentType paymentType;

    @Column(name = "loan_emi_id")
    private UUID loanEmiId;

    @Column(name = "card_statement_id")
    private UUID cardStatementId;

    @Column(name = "payment_method_id")
    private UUID paymentMethodId;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Column(name = "paid_at", nullable = false)
    private OffsetDateTime paidAt;

    @Generated(event = EventType.INSERT)
    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Generated(event = {EventType.INSERT, EventType.UPDATE})
    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime updatedAt;

    protected Payment() {
        // Required by JPA.
    }

    public Payment(UUID userId, PaymentType paymentType, UUID loanEmiId, UUID cardStatementId,
                   UUID paymentMethodId, BigDecimal amount, OffsetDateTime paidAt) {
        this.userId = userId;
        this.paymentType = paymentType;
        this.loanEmiId = loanEmiId;
        this.cardStatementId = cardStatementId;
        this.paymentMethodId = paymentMethodId;
        this.amount = amount;
        this.paidAt = paidAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public PaymentType getPaymentType() {
        return paymentType;
    }

    public UUID getLoanEmiId() {
        return loanEmiId;
    }

    public UUID getCardStatementId() {
        return cardStatementId;
    }

    public UUID getPaymentMethodId() {
        return paymentMethodId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public OffsetDateTime getPaidAt() {
        return paidAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
