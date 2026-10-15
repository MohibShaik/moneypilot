package com.moneypilot.creditcard;

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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.generator.EventType;
import org.hibernate.type.SqlTypes;

/**
 * JPA entity mapped to the V2 {@code credit_cards} table.
 *
 * <p>Conventions match the other entities: {@code id}, {@code created_at}, and
 * {@code updated_at} are database-generated ({@code insertable=false} +
 * {@link Generated}); enums are stored as strings; {@code credit_limit} is a
 * {@link BigDecimal}; the SMALLINT day columns map to {@link Short}.
 * {@code last_four} is immutable after creation.
 */
@Entity
@Table(name = "credit_cards")
public class CreditCard {

    @Id
    @Generated(event = EventType.INSERT)
    @Column(name = "id", columnDefinition = "uuid", nullable = false, updatable = false,
            insertable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "card_name", nullable = false)
    private String cardName;

    @Column(name = "bank", nullable = false)
    private String bank;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "last_four", nullable = false, updatable = false, length = 4)
    private String lastFour;

    @Column(name = "credit_limit", nullable = false)
    private BigDecimal creditLimit;

    @Column(name = "statement_day", nullable = false)
    private Short statementDay;

    @Column(name = "payment_due_day", nullable = false)
    private Short paymentDueDay;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private CardStatus status;

    @Generated(event = EventType.INSERT)
    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Generated(event = {EventType.INSERT, EventType.UPDATE})
    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime updatedAt;

    protected CreditCard() {
        // Required by JPA.
    }

    /** Creates a new ACTIVE credit card. Generated columns are set by the DB. */
    public CreditCard(UUID userId, String cardName, String bank, String lastFour,
                      BigDecimal creditLimit, Short statementDay, Short paymentDueDay) {
        this.userId = userId;
        this.cardName = cardName;
        this.bank = bank;
        this.lastFour = lastFour;
        this.creditLimit = creditLimit;
        this.statementDay = statementDay;
        this.paymentDueDay = paymentDueDay;
        this.status = CardStatus.ACTIVE;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getCardName() {
        return cardName;
    }

    public void setCardName(String cardName) {
        this.cardName = cardName;
    }

    public String getBank() {
        return bank;
    }

    public void setBank(String bank) {
        this.bank = bank;
    }

    public String getLastFour() {
        return lastFour;
    }

    public BigDecimal getCreditLimit() {
        return creditLimit;
    }

    public void setCreditLimit(BigDecimal creditLimit) {
        this.creditLimit = creditLimit;
    }

    public Short getStatementDay() {
        return statementDay;
    }

    public void setStatementDay(Short statementDay) {
        this.statementDay = statementDay;
    }

    public Short getPaymentDueDay() {
        return paymentDueDay;
    }

    public void setPaymentDueDay(Short paymentDueDay) {
        this.paymentDueDay = paymentDueDay;
    }

    public CardStatus getStatus() {
        return status;
    }

    public void setStatus(CardStatus status) {
        this.status = status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
