package com.moneypilot.category;

/**
 * Raised when a user already has a category with the same name. Thrown by the
 * fast-path check or by translating a {@code uq_categories_user_name} unique
 * violation; maps to {@code 409 DUPLICATE_CATEGORY}.
 */
public class DuplicateCategoryException extends RuntimeException {

    public DuplicateCategoryException() {
        super("A category with this name already exists");
    }
}
