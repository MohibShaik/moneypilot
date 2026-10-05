package com.moneypilot.paymentmethod;

import com.moneypilot.common.ApiResponse;
import com.moneypilot.paymentmethod.dto.CreatePaymentMethodRequest;
import com.moneypilot.paymentmethod.dto.PaymentMethodResponse;
import com.moneypilot.paymentmethod.dto.UpdatePaymentMethodRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Payment method endpoints ({@code /api/v1/payment-methods}).
 *
 * <p>Thin controller: resolves the authenticated user id from the security
 * principal (JWT subject) and delegates to {@link PaymentMethodService}. The
 * user id is never taken from the request, so a client cannot act on another
 * user's payment methods.
 */
@RestController
@RequestMapping("/api/v1/payment-methods")
@Tag(name = "Payment Methods", description = "The user's payment methods (CASH, UPI, CREDIT_CARD, etc.).")
public class PaymentMethodController {

    private final PaymentMethodService service;

    public PaymentMethodController(PaymentMethodService service) {
        this.service = service;
    }

    private static UUID currentUserId(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }

    @GetMapping
    @Operation(summary = "List payment methods")
    public ApiResponse<List<PaymentMethodResponse>> list(Authentication authentication) {
        return ApiResponse.ok(service.list(currentUserId(authentication)));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a payment method")
    public ApiResponse<PaymentMethodResponse> create(
            Authentication authentication,
            @Valid @RequestBody CreatePaymentMethodRequest request) {
        return ApiResponse.ok(service.create(currentUserId(authentication), request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a payment method")
    public ApiResponse<PaymentMethodResponse> update(
            Authentication authentication,
            @PathVariable UUID id,
            @Valid @RequestBody UpdatePaymentMethodRequest request) {
        return ApiResponse.ok(service.update(currentUserId(authentication), id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a payment method")
    public void delete(Authentication authentication, @PathVariable UUID id) {
        service.delete(currentUserId(authentication), id);
    }
}
