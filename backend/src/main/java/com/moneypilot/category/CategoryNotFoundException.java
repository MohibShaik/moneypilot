package com.moneypilot.category;

/**
 * Raised when a category does not exist, or exists but is not owned by the
 * requesting user. Both cases map to {@code 404 CATEGORY_NOT_FOUND} so the
 * response does not reveal whether another user's category id exists
 * (ownership is enforced server-side).
 */
public class CategoryNotFoundException extends RuntimeException {

    public CategoryNotFoundException() {
        super("Category not found");
    }
}
