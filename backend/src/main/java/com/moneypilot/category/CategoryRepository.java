package com.moneypilot.category;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID> {

    /**
     * Categories visible to a user: system/default categories ({@code user_id}
     * is null) plus the user's own, ordered by name.
     */
    @Query("""
            SELECT c FROM Category c
            WHERE c.userId IS NULL OR c.userId = :userId
            ORDER BY c.name ASC
            """)
    List<Category> findVisibleForUser(@Param("userId") UUID userId);

    /** Finds a category only if it is owned by the given user. */
    Optional<Category> findByIdAndUserId(UUID id, UUID userId);

    /**
     * Finds a category visible to the user by id: a system/default category
     * ({@code user_id} is null) or one owned by the user. Used by other modules
     * (e.g. Expenses) to validate a referenced category.
     */
    @Query("""
            SELECT c FROM Category c
            WHERE c.id = :id AND (c.userId IS NULL OR c.userId = :userId)
            """)
    Optional<Category> findVisibleByIdForUser(@Param("id") UUID id, @Param("userId") UUID userId);

    /** Whether the user already has a category with this name (case-sensitive). */
    boolean existsByUserIdAndName(UUID userId, String name);
}
