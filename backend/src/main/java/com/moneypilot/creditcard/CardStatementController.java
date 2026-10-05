package com.moneypilot.creditcard;

import com.moneypilot.common.ApiResponse;
import com.moneypilot.creditcard.dto.CardStatementResponse;
import com.moneypilot.creditcard.dto.CreateCardStatementRequest;
import com.moneypilot.creditcard.dto.UpdateCardStatementRequest;
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
 * Card statement endpoints, nested under a card
 * ({@code /api/v1/credit-cards/{cardId}/statements}).
 *
 * <p>Thin controller: resolves the authenticated user id from the JWT subject
 * and delegates to {@link CardStatementService}, which enforces ownership
 * through the parent card.
 */
@RestController
@RequestMapping("/api/v1/credit-cards/{cardId}/statements")
@Tag(name = "Card Statements", description = "Statements for a credit card.")
public class CardStatementController {

    private final CardStatementService service;

    public CardStatementController(CardStatementService service) {
        this.service = service;
    }

    private static UUID currentUserId(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }

    @GetMapping
    @Operation(summary = "List statements for a card")
    public ApiResponse<List<CardStatementResponse>> list(
            Authentication authentication, @PathVariable UUID cardId) {
        return ApiResponse.ok(service.list(currentUserId(authentication), cardId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a statement")
    public ApiResponse<CardStatementResponse> create(
            Authentication authentication,
            @PathVariable UUID cardId,
            @Valid @RequestBody CreateCardStatementRequest request) {
        return ApiResponse.ok(service.create(currentUserId(authentication), cardId, request));
    }

    @PutMapping("/{statementId}")
    @Operation(summary = "Update a statement", description = "statementDate is immutable.")
    public ApiResponse<CardStatementResponse> update(
            Authentication authentication,
            @PathVariable UUID cardId,
            @PathVariable UUID statementId,
            @Valid @RequestBody UpdateCardStatementRequest request) {
        return ApiResponse.ok(
                service.update(currentUserId(authentication), cardId, statementId, request));
    }

    @DeleteMapping("/{statementId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a statement")
    public void delete(
            Authentication authentication,
            @PathVariable UUID cardId,
            @PathVariable UUID statementId) {
        service.delete(currentUserId(authentication), cardId, statementId);
    }
}
