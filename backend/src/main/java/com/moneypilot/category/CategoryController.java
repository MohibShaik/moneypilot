package com.moneypilot.category;

import com.moneypilot.category.dto.CategoryResponse;
import com.moneypilot.category.dto.CreateCategoryRequest;
import com.moneypilot.category.dto.UpdateCategoryRequest;
import com.moneypilot.common.ApiResponse;
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
 * Category endpoints (HLD §13 / future {@code /api/v1/categories}).
 *
 * <p>Thin controller: it resolves the authenticated user id from the security
 * principal (the JWT subject) and delegates to {@link CategoryService}. The
 * user id is never taken from the request body or a path/query parameter, so a
 * client cannot act on another user's categories by changing an identifier.
 */
@RestController
@RequestMapping("/api/v1/categories")
@Tag(name = "Categories", description = "Expense/income categories (system defaults plus the user's own).")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    private static UUID currentUserId(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }

    @GetMapping
    @Operation(summary = "List categories")
    public ApiResponse<List<CategoryResponse>> list(Authentication authentication) {
        return ApiResponse.ok(categoryService.list(currentUserId(authentication)));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a category")
    public ApiResponse<CategoryResponse> create(
            Authentication authentication,
            @Valid @RequestBody CreateCategoryRequest request) {
        return ApiResponse.ok(categoryService.create(currentUserId(authentication), request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a category")
    public ApiResponse<CategoryResponse> update(
            Authentication authentication,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCategoryRequest request) {
        return ApiResponse.ok(categoryService.update(currentUserId(authentication), id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a category")
    public void delete(Authentication authentication, @PathVariable UUID id) {
        categoryService.delete(currentUserId(authentication), id);
    }
}
