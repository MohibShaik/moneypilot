package com.moneypilot.dashboard;

import com.moneypilot.common.ApiResponse;
import com.moneypilot.dashboard.dto.DashboardResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Dashboard endpoint ({@code /api/v1/dashboard}).
 *
 * <p>Thin, read-only controller: resolves the authenticated user id from the
 * security principal (JWT subject) and returns the aggregated summary. Never
 * 404s — an empty dataset yields zeros and an empty dues list.
 */
@RestController
@RequestMapping("/api/v1/dashboard")
@Tag(name = "Dashboard", description = "Aggregated financial summary for the home screen.")
public class DashboardController {

    private final DashboardService service;

    public DashboardController(DashboardService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Get dashboard summary")
    public ApiResponse<DashboardResponse> get(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        return ApiResponse.ok(service.getSummary(userId));
    }
}
