package com.moneypilot.category.dto;

import com.moneypilot.category.CategoryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request to create a user-owned category.
 */
public record CreateCategoryRequest(

        @NotBlank
        @Size(min = 1, max = 100)
        String name,

        @NotNull
        CategoryType categoryType
) {
}
