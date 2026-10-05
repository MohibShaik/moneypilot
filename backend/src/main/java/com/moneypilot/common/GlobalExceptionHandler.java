package com.moneypilot.common;

import com.moneypilot.auth.EmailAlreadyUsedException;
import com.moneypilot.auth.InvalidCredentialsException;
import com.moneypilot.category.CategoryNotFoundException;
import com.moneypilot.category.DuplicateCategoryException;
import com.moneypilot.creditcard.CardStatementNotFoundException;
import com.moneypilot.creditcard.CreditCardNotFoundException;
import com.moneypilot.creditcard.DuplicateCardStatementException;
import com.moneypilot.expense.ExpenseNotFoundException;
import com.moneypilot.loan.LoanNotFoundException;
import com.moneypilot.payment.PaymentNotFoundException;
import com.moneypilot.paymentmethod.PaymentMethodNotFoundException;
import com.moneypilot.user.AuthenticatedUserNotFoundException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Foundation global exception handler translating framework exceptions into the
 * consistent {@link ApiResponse} error envelope.
 *
 * <p>Covers validation failures, malformed request bodies, duplicate-email and
 * generic data-integrity conflicts, and an unexpected-error fallback. Further
 * credential/authentication handlers are added in later tasks alongside the
 * exceptions they map, so no raw Spring error page is returned for these cases.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Bean Validation failures on {@code @Valid} request bodies become a
     * {@code 400 VALIDATION_ERROR} with field-level messages.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            // Keep the first message per field; do not leak submitted values.
            fieldErrors.putIfAbsent(
                    fieldError.getField(),
                    fieldError.getDefaultMessage());
        }
        ApiError error = ApiError.of("VALIDATION_ERROR", "Validation failed", fieldErrors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(error));
    }

    /**
     * A malformed/unreadable JSON body becomes a {@code 400 BAD_REQUEST}.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Object>> handleUnreadable(HttpMessageNotReadableException ex) {
        ApiError error = ApiError.of("BAD_REQUEST", "Malformed request body");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(error));
    }

    /**
     * A duplicate-email registration becomes a {@code 409 EMAIL_ALREADY_USED}.
     * The message is generic and never includes the database constraint name.
     */
    @ExceptionHandler(EmailAlreadyUsedException.class)
    public ResponseEntity<ApiResponse<Object>> handleEmailAlreadyUsed(EmailAlreadyUsedException ex) {
        ApiError error = ApiError.of("EMAIL_ALREADY_USED", "Email already used");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error(error));
    }

    /**
     * A failed login becomes a {@code 401 INVALID_CREDENTIALS}. The message is
     * generic and identical for unknown-email and wrong-password cases.
     */
    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiResponse<Object>> handleInvalidCredentials(InvalidCredentialsException ex) {
        ApiError error = ApiError.of("INVALID_CREDENTIALS", "Invalid email or password");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(error));
    }

    /**
     * An authenticated principal that no longer resolves to a user becomes a
     * {@code 401 UNAUTHORIZED}. The response does not reveal whether the id
     * existed.
     */
    @ExceptionHandler(AuthenticatedUserNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleAuthUserNotFound(AuthenticatedUserNotFoundException ex) {
        ApiError error = ApiError.of("UNAUTHORIZED", "Authentication required");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(error));
    }

    /**
     * A missing or non-owned category becomes a {@code 404 CATEGORY_NOT_FOUND}.
     * Not-owned is reported as not-found so another user's id is not revealed.
     */
    @ExceptionHandler(CategoryNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleCategoryNotFound(CategoryNotFoundException ex) {
        ApiError error = ApiError.of("CATEGORY_NOT_FOUND", "Category not found");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(error));
    }

    /**
     * A duplicate category name for the same user becomes a
     * {@code 409 DUPLICATE_CATEGORY}. The constraint name is never exposed.
     */
    @ExceptionHandler(DuplicateCategoryException.class)
    public ResponseEntity<ApiResponse<Object>> handleDuplicateCategory(DuplicateCategoryException ex) {
        ApiError error = ApiError.of("DUPLICATE_CATEGORY", "A category with this name already exists");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error(error));
    }

    /**
     * A missing or non-owned payment method becomes a
     * {@code 404 PAYMENT_METHOD_NOT_FOUND}. Not-owned is reported as not-found.
     */
    @ExceptionHandler(PaymentMethodNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handlePaymentMethodNotFound(PaymentMethodNotFoundException ex) {
        ApiError error = ApiError.of("PAYMENT_METHOD_NOT_FOUND", "Payment method not found");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(error));
    }

    /**
     * A missing or non-owned expense becomes a {@code 404 EXPENSE_NOT_FOUND}.
     */
    @ExceptionHandler(ExpenseNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleExpenseNotFound(ExpenseNotFoundException ex) {
        ApiError error = ApiError.of("EXPENSE_NOT_FOUND", "Expense not found");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(error));
    }

    /**
     * A missing or non-owned payment becomes a {@code 404 PAYMENT_NOT_FOUND}.
     */
    @ExceptionHandler(PaymentNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handlePaymentNotFound(PaymentNotFoundException ex) {
        ApiError error = ApiError.of("PAYMENT_NOT_FOUND", "Payment not found");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(error));
    }

    /**
     * A missing or non-owned loan becomes a {@code 404 LOAN_NOT_FOUND}.
     * Not-owned is reported as not-found so another user's id is not revealed.
     */
    @ExceptionHandler(LoanNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleLoanNotFound(LoanNotFoundException ex) {
        ApiError error = ApiError.of("LOAN_NOT_FOUND", "Loan not found");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(error));
    }

    /**
     * A missing or non-owned credit card becomes a
     * {@code 404 CREDIT_CARD_NOT_FOUND}. Not-owned is reported as not-found.
     */
    @ExceptionHandler(CreditCardNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleCreditCardNotFound(CreditCardNotFoundException ex) {
        ApiError error = ApiError.of("CREDIT_CARD_NOT_FOUND", "Credit card not found");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(error));
    }

    /**
     * A missing statement, or one not belonging to an owned card, becomes a
     * {@code 404 CARD_STATEMENT_NOT_FOUND}.
     */
    @ExceptionHandler(CardStatementNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleCardStatementNotFound(
            CardStatementNotFoundException ex) {
        ApiError error = ApiError.of("CARD_STATEMENT_NOT_FOUND", "Card statement not found");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(error));
    }

    /**
     * A duplicate statement date for a card becomes a
     * {@code 409 DUPLICATE_CARD_STATEMENT}. The constraint name is never exposed.
     */
    @ExceptionHandler(DuplicateCardStatementException.class)
    public ResponseEntity<ApiResponse<Object>> handleDuplicateCardStatement(
            DuplicateCardStatementException ex) {
        ApiError error = ApiError.of(
                "DUPLICATE_CARD_STATEMENT",
                "A statement with this date already exists for the card");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error(error));
    }

    /**
     * A well-formed request that violates a cross-field business rule becomes a
     * {@code 400 BAD_REQUEST} carrying the rule's message.
     */
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiResponse<Object>> handleBadRequest(BadRequestException ex) {
        ApiError error = ApiError.of("BAD_REQUEST", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(error));
    }

    /**
     * Any data-integrity violation not already classified (e.g. not a
     * duplicate-email case) becomes a generic {@code 409 DATA_INTEGRITY_VIOLATION}.
     * The database constraint name is never exposed to the client.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Object>> handleDataIntegrity(DataIntegrityViolationException ex) {
        ApiError error = ApiError.of("DATA_INTEGRITY_VIOLATION", "Request could not be completed");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error(error));
    }

    /**
     * Fallback: any otherwise unhandled exception becomes a
     * {@code 500 INTERNAL_ERROR} with a generic message (no stack trace or
     * internal detail is exposed).
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleUnexpected(Exception ex) {
        ApiError error = ApiError.of("INTERNAL_ERROR", "An unexpected error occurred");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error(error));
    }
}
