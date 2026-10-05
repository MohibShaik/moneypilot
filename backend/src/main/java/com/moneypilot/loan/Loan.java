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
 * JPA entity mapped to the V2 {@code loans} table.
 *
 * <p>Conventions match the other entities: {@code id}, {@code created_at}, and
 * {@code updated_at} are database-generated ({@code insertable=false} +
 * {@link Generated}) and read back after insert; Hibernate runs with
 * {@code ddl-auto=validate}. Enums are stored as strings; monetary values are
 * {@link BigDecimal}; business dates are {@link LocalDate}.
 *
 * <p>The loan ↔ EMI relationship is modeled by id ({@link LoanEmi#getLoanId()}),
 * not with a JPA object graph, to keep the write path explicit.
 */
@Entity
@Table(name = "loans")
public class Loan {

    @Id
    @Generated(event = EventType.INSERT)
    @Column(name = "id", columnDefinition = "uuid", nullable = false, updatable = false,
            insertable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "lender")
    private String lender;

    @Enumerated(EnumType.STRING)
    @Column(name = "loan_type", nullable = false)
    private LoanType loanType;

    @Column(name = "principal_amount", nullable = false)
    private BigDecimal principalAmount;

    @Column(name = "outstanding_principal", nullable = false)
    private BigDecimal outstandingPrincipal;

    @Column(name = "emi_amount", nullable = false)
    private BigDecimal emiAmount;

    @Column(name = "interest_rate", nullable = false)
    private BigDecimal interestRate;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "tenure_months", nullable = false)
    private Integer tenureMonths;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private LoanStatus status;

    @Generated(event = EventType.INSERT)
    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Generated(event = {EventType.INSERT, EventType.UPDATE})
    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime updatedAt;

    protected Loan() {
        // Required by JPA.
    }

    /**
     * Creates a new ACTIVE loan. Outstanding principal starts equal to the
     * principal amount. The generated columns are populated by the database
     * after save.
     */
    public Loan(UUID userId, String name, String lender, LoanType loanType,
                BigDecimal principalAmount, BigDecimal emiAmount, BigDecimal interestRate,
                LocalDate startDate, LocalDate endDate, Integer tenureMonths) {
        this.userId = userId;
        this.name = name;
        this.lender = lender;
        this.loanType = loanType;
        this.principalAmount = principalAmount;
        this.outstandingPrincipal = principalAmount;
        this.emiAmount = emiAmount;
        this.interestRate = interestRate;
        this.startDate = startDate;
        this.endDate = endDate;
        this.tenureMonths = tenureMonths;
        this.status = LoanStatus.ACTIVE;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLender() {
        return lender;
    }

    public void setLender(String lender) {
        this.lender = lender;
    }

    public LoanType getLoanType() {
        return loanType;
    }

    public BigDecimal getPrincipalAmount() {
        return principalAmount;
    }

    public BigDecimal getOutstandingPrincipal() {
        return outstandingPrincipal;
    }

    public void setOutstandingPrincipal(BigDecimal outstandingPrincipal) {
        this.outstandingPrincipal = outstandingPrincipal;
    }

    public BigDecimal getEmiAmount() {
        return emiAmount;
    }

    public BigDecimal getInterestRate() {
        return interestRate;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public Integer getTenureMonths() {
        return tenureMonths;
    }

    public LoanStatus getStatus() {
        return status;
    }

    public void setStatus(LoanStatus status) {
        this.status = status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
