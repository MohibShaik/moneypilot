package com.moneypilot.category.dto;

import com.moneypilot.category.CategoryType;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Public category representation. {@code system} is true for shared
 * system/default categories (read-only to users); false for user-owned ones.
 */
public record CategoryResponse(
        UUID id,
        String name,
        CategoryType categoryType,
        boolean system,
        OffsetDateTime createdAt
) {
}
