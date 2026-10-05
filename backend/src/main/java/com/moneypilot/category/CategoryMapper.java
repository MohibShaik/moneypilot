package com.moneypilot.category;

import com.moneypilot.category.dto.CategoryResponse;
import org.springframework.stereotype.Component;

/**
 * Maps {@link Category} entities to their public {@link CategoryResponse} DTO.
 */
@Component
public class CategoryMapper {

    public CategoryResponse toResponse(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getCategoryType(),
                category.isSystem(),
                category.getCreatedAt());
    }
}
