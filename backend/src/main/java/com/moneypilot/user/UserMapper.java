package com.moneypilot.user;

import com.moneypilot.user.dto.UserResponse;
import org.springframework.stereotype.Component;

/**
 * Maps the {@link User} entity to its public {@link UserResponse} DTO.
 *
 * <p>The mapping is an explicit field-by-field copy and intentionally omits
 * {@code passwordHash}: there is no code path here that reads or copies the
 * password hash, so it can never leak into an API response.
 */
@Component
public class UserMapper {

    public UserResponse toResponse(User user) {
        if (user == null) {
            return null;
        }
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getCurrency(),
                user.getTimezone(),
                user.getCreatedAt()
        );
    }
}
