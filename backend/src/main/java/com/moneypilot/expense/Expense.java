package com.moneypilot.expense;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

/**
 * JPA entity mapped to the V2 {@code expenses} table.
 *
 * <p>Conventions match the other entities: {@code id}, {@code created_at}, and
 * {@code updated_at} are database-generated ({@code insertable=false} +
 * {@link Generated}); {@code amount} is a {@link BigDecimal}; {@code
 * expense_date} is a {@link LocalDate}. The optional category and payment
 * method are modeled by nullable UUID columns (not an object graph), consistent
 * with the other modules. Their ownership/visibility is validated in the
 * service before persisting.
 */
@Entity
@Table(name = "expenses")
public class Expense {

    @Id
    @Generated(event = EventType.INSERT)
    @Column(name = "id", columnDefinition = "uuid", nullable = false, updatable = false,
            insertable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "category_id")
    private UUID categoryId;

    @Column(name = "payment_method_id")
    private UUID paymentMethodId;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Column(name = "expense_date", nullable = false)
    private LocalDate expenseDate;

    @Column(name = "description")
    private String description;

    @Column(name = "notes")
    private String notes;

    @Generated(event = EventType.INSERT)
    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Generated(event = {EventType.INSERT, EventType.UPDATE})
    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime updatedAt;

    protected Expense() {
        // Required by JPA.
    }

    public Expense(UUID userId, UUID categoryId, UUID paymentMethodId, BigDecimal amount,
                   LocalDate expenseDate, String description, String notes) {
        this.userId = userId;
        this.categoryId = categoryId;
        this.paymentMethodId = paymentMethodId;
        this.amount = amount;
        this.expenseDate = expenseDate;
        this.description = description;
        this.notes = notes;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(UUID categoryId) {
        this.categoryId = categoryId;
    }

    public UUID getPaymentMethodId() {
        return paymentMethodId;
    }

    public void setPaymentMethodId(UUID paymentMethodId) {
        this.paymentMethodId = paymentMethodId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public LocalDate getExpenseDate() {
        return expenseDate;
    }

    public void setExpenseDate(LocalDate expenseDate) {
        this.expenseDate = expenseDate;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
