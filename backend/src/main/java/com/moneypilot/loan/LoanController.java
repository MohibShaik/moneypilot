package com.moneypilot.loan;

import com.moneypilot.common.ApiResponse;
import com.moneypilot.loan.dto.CreateLoanRequest;
import com.moneypilot.loan.dto.LoanResponse;
import com.moneypilot.loan.dto.UpdateLoanRequest;
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
 * Loan endpoints ({@code /api/v1/loans}).
 *
 * <p>Thin controller: resolves the authenticated user id from the security
 * principal (JWT subject) and delegates to {@link LoanService}. The user id is
 * never taken from the request, so a client cannot act on another user's loans.
 */
@RestController
@RequestMapping("/api/v1/loans")
@Tag(name = "Loans", description = "Loans and their generated EMI schedules.")
public class LoanController {

    private final LoanService service;

    public LoanController(LoanService service) {
        this.service = service;
    }

    private static UUID currentUserId(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }

    @GetMapping
    @Operation(summary = "List loans", description = "Loan summaries (without the EMI schedule).")
    public ApiResponse<List<LoanResponse>> list(Authentication authentication) {
        return ApiResponse.ok(service.list(currentUserId(authentication)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a loan", description = "Loan detail including its full EMI schedule.")
    public ApiResponse<LoanResponse> get(Authentication authentication, @PathVariable UUID id) {
        return ApiResponse.ok(service.get(currentUserId(authentication), id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a loan",
            description = "Creates a loan; the EMI schedule is generated server-side.")
    public ApiResponse<LoanResponse> create(
            Authentication authentication,
            @Valid @RequestBody CreateLoanRequest request) {
        return ApiResponse.ok(service.create(currentUserId(authentication), request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a loan",
            description = "Updates name, lender, and status. Financial terms are immutable.")
    public ApiResponse<LoanResponse> update(
            Authentication authentication,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateLoanRequest request) {
        return ApiResponse.ok(service.update(currentUserId(authentication), id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a loan", description = "Deletes the loan and its EMI schedule.")
    public void delete(Authentication authentication, @PathVariable UUID id) {
        service.delete(currentUserId(authentication), id);
    }
}
