/**
 * Category module: expense/income categories.
 *
 * <p>A category with {@code user_id = NULL} is a system/default category shared
 * across all users; a category with a {@code user_id} is owned by that user
 * (backend V2 schema semantics). Users may read system categories and manage
 * their own; they may never read or modify another user's categories.
 */
package com.moneypilot.category;
