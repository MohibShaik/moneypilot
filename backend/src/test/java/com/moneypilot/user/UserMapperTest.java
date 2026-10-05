package com.moneypilot.user;

import static org.assertj.core.api.Assertions.assertThat;

import com.moneypilot.user.dto.UserResponse;
import java.lang.reflect.Field;
import java.lang.reflect.RecordComponent;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UserMapperTest {

    private final UserMapper mapper = new UserMapper();

    @Test
    void mapsAllPublicProfileFields() throws Exception {
        UUID id = UUID.randomUUID();
        OffsetDateTime createdAt = OffsetDateTime.of(2026, 10, 3, 10, 0, 0, 0, ZoneOffset.UTC);

        User user = new User();
        user.setId(id);
        user.setEmail("ada@example.com");
        user.setDisplayName("Ada");
        user.setPasswordHash("$argon2id$v=19$m=19456,t=2,p=1$abc$def");
        user.setCurrency("INR");
        user.setTimezone("Asia/Kolkata");
        setField(user, "createdAt", createdAt);

        UserResponse response = mapper.toResponse(user);

        assertThat(response.id()).isEqualTo(id);
        assertThat(response.email()).isEqualTo("ada@example.com");
        assertThat(response.displayName()).isEqualTo("Ada");
        assertThat(response.currency()).isEqualTo("INR");
        assertThat(response.timezone()).isEqualTo("Asia/Kolkata");
        assertThat(response.createdAt()).isEqualTo(createdAt);
    }

    @Test
    void returnsNullForNullInput() {
        assertThat(mapper.toResponse(null)).isNull();
    }

    @Test
    void userResponseHasNoPasswordComponent() {
        // Guard against a password hash ever being added to the public DTO.
        for (RecordComponent component : UserResponse.class.getRecordComponents()) {
            assertThat(component.getName().toLowerCase())
                    .doesNotContain("password")
                    .doesNotContain("hash");
        }
    }

    @Test
    void mappedResponseDoesNotCarryThePasswordHashValue() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("ada@example.com");
        user.setDisplayName("Ada");
        String secret = "$argon2id$v=19$m=19456,t=2,p=1$SALTSALT$HASHHASH";
        user.setPasswordHash(secret);
        user.setCurrency("INR");
        user.setTimezone("Asia/Kolkata");

        UserResponse response = mapper.toResponse(user);

        assertThat(response.toString()).doesNotContain(secret);
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
