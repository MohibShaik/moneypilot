package com.moneypilot.creditcard;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

/**
 * JPA entity mapped to the V2 {@code card_statements} table.
 *
 * <p>Same DB-generated-column conventions as {@link CreditCard}. The parent
 * card is referenced by {@link #creditCardId}. {@code statementDate} is
 * immutable after creation (it is the unique key with the card). The DB CHECKs
 * enforce amount ranges and {@code due_date >= statement_date}.
 */
@Entity
@Table(name = "card_statements")
public class CardStatement {

    @Id
    @Generated(event = EventType.INSERT)
    @Column(name = "id", columnDefinition = "uuid", nullable = false, updatable = false,
            insertable = false)
    private UUID id;

    @Column(name = "credit_card_id", nullable = false)
    private UUID creditCardId;

    @Column(name = "statement_date", nullable = false, updatable = false)
    private LocalDate statementDate;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "statement_amount", nullable = false)
    private BigDecimal statementAmount;

    @Column(name = "minimum_due", nullable = false)
    private BigDecimal minimumDue;

    @Column(name = "paid_amount", nullable = false)
    private BigDecimal paidAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private StatementStatus status;

    @Generated(event = EventType.INSERT)
    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Generated(event = {EventType.INSERT, EventType.UPDATE})
    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime updatedAt;

    protected CardStatement() {
        // Required by JPA.
    }

    /**
     * Creates a statement for a card. The caller supplies the derived or
     * explicit {@code status}. Generated columns are set by the DB.
     */
    public CardStatement(UUID creditCardId, LocalDate statementDate, LocalDate dueDate,
                         BigDecimal statementAmount, BigDecimal minimumDue, BigDecimal paidAmount,
                         StatementStatus status) {
        this.creditCardId = creditCardId;
        this.statementDate = statementDate;
        this.dueDate = dueDate;
        this.statementAmount = statementAmount;
        this.minimumDue = minimumDue;
        this.paidAmount = paidAmount;
        this.status = status;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCreditCardId() {
        return creditCardId;
    }

    public LocalDate getStatementDate() {
        return statementDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public BigDecimal getStatementAmount() {
        return statementAmount;
    }

    public void setStatementAmount(BigDecimal statementAmount) {
        this.statementAmount = statementAmount;
    }

    public BigDecimal getMinimumDue() {
        return minimumDue;
    }

    public void setMinimumDue(BigDecimal minimumDue) {
        this.minimumDue = minimumDue;
    }

    public BigDecimal getPaidAmount() {
        return paidAmount;
    }

    public void setPaidAmount(BigDecimal paidAmount) {
        this.paidAmount = paidAmount;
    }

    public StatementStatus getStatus() {
        return status;
    }

    public void setStatus(StatementStatus status) {
        this.status = status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
