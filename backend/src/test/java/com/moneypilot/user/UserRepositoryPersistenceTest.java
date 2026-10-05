package com.moneypilot.user;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

/**
 * Focused persistence test for {@link UserRepository} against the configured
 * local PostgreSQL database.
 *
 * <p>{@code @AutoConfigureTestDatabase(replace = NONE)} keeps the real
 * datasource from {@code application.yml}; no embedded/H2/Testcontainers
 * database is used. Verifies that PostgreSQL-side generation (UUID primary key
 * and the {@code created_at}/{@code updated_at} defaults) is correctly surfaced
 * on the persisted entity by Hibernate 6, and that the finder methods work.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserRepositoryPersistenceTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    private User newUser(String email) {
        User user = new User();
        user.setEmail(email);
        user.setDisplayName("Ada Lovelace");
        user.setPasswordHash("$argon2id$v=19$m=19456,t=2,p=1$c2FsdHNhbHQ$aGFzaGhhc2g");
        user.setCurrency("INR");
        user.setTimezone("Asia/Kolkata");
        return user;
    }

    private String uniqueEmail() {
        return "ada+" + UUID.randomUUID() + "@example.com";
    }

    @Test
    void savesUserAndSurfacesDatabaseGeneratedValues() {
        // saveAndFlush forces the INSERT to execute now, so the UUID primary key
        // and the DB-generated created_at/updated_at (via the @Generated INSERT
        // "returning" clause) are populated on the managed entity. A plain
        // save() would defer the INSERT until flush, leaving the generated
        // timestamps unread — the registration service will likewise read the
        // row back after insert.
        User saved = userRepository.saveAndFlush(newUser(uniqueEmail()));

        // UUID generated and available on the persisted entity after save.
        assertThat(saved.getId()).isNotNull();

        // created_at / updated_at populated by the DB defaults and read back.
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();

        // Payload fields persisted as given.
        assertThat(saved.getPasswordHash())
                .isEqualTo("$argon2id$v=19$m=19456,t=2,p=1$c2FsdHNhbHQ$aGFzaGhhc2g");
        assertThat(saved.getCurrency()).isEqualTo("INR");
        assertThat(saved.getTimezone()).isEqualTo("Asia/Kolkata");
    }

    @Test
    void reloadsPersistedRowFromDatabaseWithAllFields() {
        String email = uniqueEmail();
        // Flush so the INSERT executes and the database-generated UUID is read
        // back onto the entity before we use it as the lookup key.
        User saved = userRepository.saveAndFlush(newUser(email));
        UUID id = saved.getId();
        assertThat(id).isNotNull();

        // Force a real round-trip to the database rather than first-level cache.
        entityManager.clear();

        User reloaded = userRepository.findById(id).orElseThrow();
        assertThat(reloaded.getId()).isEqualTo(id);
        assertThat(reloaded.getEmail()).isEqualTo(email);
        assertThat(reloaded.getDisplayName()).isEqualTo("Ada Lovelace");
        assertThat(reloaded.getPasswordHash())
                .isEqualTo("$argon2id$v=19$m=19456,t=2,p=1$c2FsdHNhbHQ$aGFzaGhhc2g");
        assertThat(reloaded.getCurrency()).isEqualTo("INR");
        assertThat(reloaded.getTimezone()).isEqualTo("Asia/Kolkata");
        assertThat(reloaded.getCreatedAt()).isNotNull();
        assertThat(reloaded.getUpdatedAt()).isNotNull();
    }

    @Test
    void uuidIsGeneratedByTheDatabaseOnInsertNotByTheApplication() {
        // The application never assigns the id. With database-generated UUID
        // (gen_random_uuid() default, mapped @Generated INSERT), the id remains
        // null until the INSERT actually executes at flush, then is read back
        // via the SQL "returning" clause.
        User user = newUser(uniqueEmail());
        assertThat(user.getId()).isNull();

        userRepository.save(user);
        // Not asserting null here: Spring Data may or may not flush, and the
        // contract we care about is that after flush the DB-generated id is set.

        entityManager.flush();
        assertThat(user.getId()).isNotNull();
    }

    @Test
    void findByEmailReturnsThePersistedUser() {
        String email = uniqueEmail();
        User saved = userRepository.save(newUser(email));
        entityManager.flush();
        entityManager.clear();

        Optional<User> found = userRepository.findByEmail(email);
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(saved.getId());
        assertThat(found.get().getEmail()).isEqualTo(email);
    }

    @Test
    void findByEmailIsEmptyForUnknownEmail() {
        assertThat(userRepository.findByEmail("missing+" + UUID.randomUUID() + "@example.com"))
                .isEmpty();
    }

    @Test
    void existsByEmailReflectsPersistence() {
        String email = uniqueEmail();
        assertThat(userRepository.existsByEmail(email)).isFalse();

        userRepository.save(newUser(email));
        entityManager.flush();

        assertThat(userRepository.existsByEmail(email)).isTrue();
    }
}
