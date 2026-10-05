package com.moneypilot.expense;

import com.moneypilot.common.ApiResponse;
import com.moneypilot.expense.dto.CreateExpenseRequest;
import com.moneypilot.expense.dto.ExpenseResponse;
import com.moneypilot.expense.dto.UpdateExpenseRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Expense endpoints ({@code /api/v1/expenses}).
 *
 * <p>Thin controller: resolves the authenticated user id from the security
 * principal (JWT subject) and delegates to {@link ExpenseService}. The user id
 * is never taken from the request.
 */
@RestController
@RequestMapping("/api/v1/expenses")
@Tag(name = "Expenses", description = "The user's expenses with optional category/payment-method links.")
public class ExpenseController {

    private final ExpenseService service;

    public ExpenseController(ExpenseService service) {
        this.service = service;
    }

    private static UUID currentUserId(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }

    @GetMapping
    @Operation(summary = "List expenses", description = "Optional filters: from, to (inclusive), categoryId.")
    public ApiResponse<List<ExpenseResponse>> list(
            Authentication authentication,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) UUID categoryId) {
        return ApiResponse.ok(service.list(currentUserId(authentication), from, to, categoryId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get an expense")
    public ApiResponse<ExpenseResponse> get(Authentication authentication, @PathVariable UUID id) {
        return ApiResponse.ok(service.get(currentUserId(authentication), id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create an expense")
    public ApiResponse<ExpenseResponse> create(
            Authentication authentication,
            @Valid @RequestBody CreateExpenseRequest request) {
        return ApiResponse.ok(service.create(currentUserId(authentication), request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an expense")
    public ApiResponse<ExpenseResponse> update(
            Authentication authentication,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateExpenseRequest request) {
        return ApiResponse.ok(service.update(currentUserId(authentication), id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete an expense")
    public void delete(Authentication authentication, @PathVariable UUID id) {
        service.delete(currentUserId(authentication), id);
    }
}
