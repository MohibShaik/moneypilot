package com.moneypilot.creditcard;

import com.moneypilot.common.ApiResponse;
import com.moneypilot.creditcard.dto.CreateCreditCardRequest;
import com.moneypilot.creditcard.dto.CreditCardResponse;
import com.moneypilot.creditcard.dto.UpdateCreditCardRequest;
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
 * Credit card endpoints ({@code /api/v1/credit-cards}).
 *
 * <p>Thin controller: resolves the authenticated user id from the security
 * principal (JWT subject) and delegates to {@link CreditCardService}. The user
 * id is never taken from the request.
 */
@RestController
@RequestMapping("/api/v1/credit-cards")
@Tag(name = "Credit Cards", description = "The user's credit cards.")
public class CreditCardController {

    private final CreditCardService service;

    public CreditCardController(CreditCardService service) {
        this.service = service;
    }

    private static UUID currentUserId(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }

    @GetMapping
    @Operation(summary = "List credit cards")
    public ApiResponse<List<CreditCardResponse>> list(Authentication authentication) {
        return ApiResponse.ok(service.list(currentUserId(authentication)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a credit card")
    public ApiResponse<CreditCardResponse> get(Authentication authentication, @PathVariable UUID id) {
        return ApiResponse.ok(service.get(currentUserId(authentication), id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a credit card")
    public ApiResponse<CreditCardResponse> create(
            Authentication authentication,
            @Valid @RequestBody CreateCreditCardRequest request) {
        return ApiResponse.ok(service.create(currentUserId(authentication), request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a credit card", description = "lastFour is immutable.")
    public ApiResponse<CreditCardResponse> update(
            Authentication authentication,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCreditCardRequest request) {
        return ApiResponse.ok(service.update(currentUserId(authentication), id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a credit card", description = "Deletes the card and its statements.")
    public void delete(Authentication authentication, @PathVariable UUID id) {
        service.delete(currentUserId(authentication), id);
    }
}
