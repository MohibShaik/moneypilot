package com.moneypilot.dashboard.dto;

import com.moneypilot.payment.PaymentType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * One nearest-due obligation in the dashboard's upcoming-dues list.
 *
 * <p>{@code type} and {@code referenceId} line up with the Payments API: a
 * {@code LOAN_EMI} item's {@code referenceId} is a loan-EMI id, a
 * {@code CREDIT_CARD} item's is a card-statement id, so the client can turn an
 * item directly into a {@code CreatePaymentRequest}.
 */
public record UpcomingDueItem(
        PaymentType type,
        UUID referenceId,
        String label,
        LocalDate dueDate,
        BigDecimal amountDue
) {
}
