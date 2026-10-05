package com.moneypilot.category;

import com.moneypilot.category.dto.CategoryResponse;
import com.moneypilot.category.dto.CreateCategoryRequest;
import com.moneypilot.category.dto.UpdateCategoryRequest;
import java.util.List;
import java.util.UUID;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business logic for categories.
 *
 * <p>Identity always comes from the authenticated user id passed by the
 * controller (the JWT subject), never from client-supplied identifiers.
 * Ownership is enforced on every mutating operation: a user may read system
 * categories and their own, and may only modify or delete their own. System
 * categories are read-only to users.
 */
@Service
public class CategoryService {

    /** V2 unique constraint on {@code (user_id, name)} for user-owned categories. */
    static final String USER_NAME_UNIQUE_CONSTRAINT = "uq_categories_user_name";

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    public CategoryService(CategoryRepository categoryRepository, CategoryMapper categoryMapper) {
        this.categoryRepository = categoryRepository;
        this.categoryMapper = categoryMapper;
    }

    /** Lists categories visible to the user: system/default plus their own. */
    @Transactional(readOnly = true)
    public List<CategoryResponse> list(UUID userId) {
        return categoryRepository.findVisibleForUser(userId).stream()
                .map(categoryMapper::toResponse)
                .toList();
    }

    /** Creates a user-owned category. */
    @Transactional
    public CategoryResponse create(UUID userId, CreateCategoryRequest request) {
        if (categoryRepository.existsByUserIdAndName(userId, request.name())) {
            throw new DuplicateCategoryException();
        }
        Category category = new Category(userId, request.name(), request.categoryType());
        Category saved;
        try {
            saved = categoryRepository.saveAndFlush(category);
        } catch (DataIntegrityViolationException ex) {
            if (isUserNameUniqueViolation(ex)) {
                throw new DuplicateCategoryException();
            }
            throw ex;
        }
        return categoryMapper.toResponse(saved);
    }

    /** Updates a user-owned category. System/other-user categories are not found. */
    @Transactional
    public CategoryResponse update(UUID userId, UUID categoryId, UpdateCategoryRequest request) {
        Category category = categoryRepository.findByIdAndUserId(categoryId, userId)
                .orElseThrow(CategoryNotFoundException::new);

        boolean nameChanged = !category.getName().equals(request.name());
        if (nameChanged && categoryRepository.existsByUserIdAndName(userId, request.name())) {
            throw new DuplicateCategoryException();
        }

        category.setName(request.name());
        category.setCategoryType(request.categoryType());
        Category saved;
        try {
            saved = categoryRepository.saveAndFlush(category);
        } catch (DataIntegrityViolationException ex) {
            if (isUserNameUniqueViolation(ex)) {
                throw new DuplicateCategoryException();
            }
            throw ex;
        }
        return categoryMapper.toResponse(saved);
    }

    /** Deletes a user-owned category. System/other-user categories are not found. */
    @Transactional
    public void delete(UUID userId, UUID categoryId) {
        Category category = categoryRepository.findByIdAndUserId(categoryId, userId)
                .orElseThrow(CategoryNotFoundException::new);
        categoryRepository.delete(category);
    }

    private boolean isUserNameUniqueViolation(DataIntegrityViolationException ex) {
        Throwable cause = ex.getCause();
        while (cause != null) {
            if (cause instanceof ConstraintViolationException cve) {
                String name = cve.getConstraintName();
                return name != null && name.toLowerCase().contains(USER_NAME_UNIQUE_CONSTRAINT);
            }
            cause = cause.getCause();
        }
        return false;
    }
}
