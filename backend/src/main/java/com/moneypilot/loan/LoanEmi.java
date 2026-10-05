package com.moneypilot.loan;

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
 * JPA entity mapped to the V2 {@code loan_emis} table — one installment row of
 * a loan's amortization schedule.
 *
 * <p>Same DB-generated-column conventions as {@link Loan}. The parent loan is
 * referenced by {@link #loanId}. Each row satisfies the DB CHECK
 * {@code principal_component + interest_component = emi_amount} exactly at
 * scale 2 (enforced by the schedule generator). New rows start
 * {@link EmiStatus#UPCOMING} with a null {@code paid_date}.
 */
@Entity
@Table(name = "loan_emis")
public class LoanEmi {

    @Id
    @Generated(event = EventType.INSERT)
    @Column(name = "id", columnDefinition = "uuid", nullable = false, updatable = false,
            insertable = false)
    private UUID id;

    @Column(name = "loan_id", nullable = false)
    private UUID loanId;

    @Column(name = "installment_no", nullable = false)
    private Integer installmentNo;

    @Column(name = "principal_component", nullable = false)
    private BigDecimal principalComponent;

    @Column(name = "interest_component", nullable = false)
    private BigDecimal interestComponent;

    @Column(name = "emi_amount", nullable = false)
    private BigDecimal emiAmount;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "paid_date")
    private LocalDate paidDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private EmiStatus status;

    @Generated(event = EventType.INSERT)
    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Generated(event = {EventType.INSERT, EventType.UPDATE})
    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime updatedAt;

    protected LoanEmi() {
        // Required by JPA.
    }

    /**
     * Creates an UPCOMING installment row. {@code emiAmount} must equal
     * {@code principalComponent + interestComponent} at scale 2 (DB CHECK).
     */
    public LoanEmi(UUID loanId, Integer installmentNo, BigDecimal principalComponent,
                   BigDecimal interestComponent, BigDecimal emiAmount, LocalDate dueDate) {
        this.loanId = loanId;
        this.installmentNo = installmentNo;
        this.principalComponent = principalComponent;
        this.interestComponent = interestComponent;
        this.emiAmount = emiAmount;
        this.dueDate = dueDate;
        this.status = EmiStatus.UPCOMING;
    }

    public UUID getId() {
        return id;
    }

    public UUID getLoanId() {
        return loanId;
    }

    public Integer getInstallmentNo() {
        return installmentNo;
    }

    public BigDecimal getPrincipalComponent() {
        return principalComponent;
    }

    public BigDecimal getInterestComponent() {
        return interestComponent;
    }

    public BigDecimal getEmiAmount() {
        return emiAmount;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public LocalDate getPaidDate() {
        return paidDate;
    }

    public void setPaidDate(LocalDate paidDate) {
        this.paidDate = paidDate;
    }

    public EmiStatus getStatus() {
        return status;
    }

    public void setStatus(EmiStatus status) {
        this.status = status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
