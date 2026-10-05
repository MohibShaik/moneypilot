package com.moneypilot.user;

import com.moneypilot.user.dto.UserResponse;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * User profile read operations.
 *
 * <p>Identity always comes from the authenticated principal (the JWT subject),
 * never from a client-supplied identifier. If the authenticated id does not
 * resolve to a user, that is treated as an authentication problem rather than a
 * "not found" so the response does not leak whether a given id exists.
 */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public UserService(UserRepository userRepository, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }

    /**
     * Returns the public profile for the authenticated user id.
     *
     * @param userId the authenticated user's UUID (from the JWT subject)
     * @throws AuthenticatedUserNotFoundException if no user matches the id
     */
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(AuthenticatedUserNotFoundException::new);
        return userMapper.toResponse(user);
    }
}
