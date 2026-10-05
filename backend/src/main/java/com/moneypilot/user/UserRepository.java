package com.moneypilot.user;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for {@link User}.
 *
 * <p>{@code existsByEmail} is a fast pre-check for registration UX; the database
 * unique constraint {@code uq_users_email} remains the source of truth for
 * concurrent registration races.
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}
