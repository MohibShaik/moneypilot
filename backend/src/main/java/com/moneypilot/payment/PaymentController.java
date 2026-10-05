package com.moneypilot.payment;

import com.moneypilot.common.ApiResponse;
import com.moneypilot.payment.dto.CreatePaymentRequest;
import com.moneypilot.payment.dto.PaymentResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Payment endpoints ({@code /api/v1/payments}).
 *
 * <p>Thin controller: resolves the authenticated user id from the security
 * principal (JWT subject) and delegates to {@link PaymentService}. Payments are
 * immutable, so there is no update endpoint; correct a payment with delete +
 * re-create.
 */
@RestController
@RequestMapping("/api/v1/payments")
@Tag(name = "Payments", description = "Payments against loan EMIs, card statements, or untargeted (OTHER).")
public class PaymentController {

    private final PaymentService service;

    public PaymentController(PaymentService service) {
        this.service = service;
    }

    private static UUID currentUserId(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }

    @GetMapping
    @Operation(summary = "List payments", description = "Optional filters: type, from, to (on paidAt).")
    public ApiResponse<List<PaymentResponse>> list(
            Authentication authentication,
            @RequestParam(required = false) PaymentType type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to) {
        return ApiResponse.ok(service.list(currentUserId(authentication), type, from, to));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a payment")
    public ApiResponse<PaymentResponse> get(Authentication authentication, @PathVariable UUID id) {
        return ApiResponse.ok(service.get(currentUserId(authentication), id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Record a payment",
            description = "Records a payment and settles the targeted EMI or statement.")
    public ApiResponse<PaymentResponse> create(
            Authentication authentication,
            @Valid @RequestBody CreatePaymentRequest request) {
        return ApiResponse.ok(service.create(currentUserId(authentication), request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a payment", description = "Deletes the payment and re-settles its target.")
    public void delete(Authentication authentication, @PathVariable UUID id) {
        service.delete(currentUserId(authentication), id);
    }
}
