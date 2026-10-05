package com.moneypilot.auth;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Provides the application's {@link PasswordEncoder} bean.
 *
 * <p>Per the security architecture, passwords are hashed with <b>Argon2id</b>.
 * {@link Argon2PasswordEncoder#defaultsForSpringSecurity_v5_8()} produces the
 * Argon2id variant with the parameters recommended by Spring Security
 * (saltLength=16, hashLength=32, parallelism=1, memory=19456 KiB, iterations=2).
 * The encoded output has the form {@code $argon2id$v=19$m=19456,t=2,p=1$...}
 * which fits the {@code password_hash VARCHAR(255)} column.
 *
 * <p>Argon2 accepts arbitrary-length input, so passwords (validated 8–128) are
 * never truncated. The raw password is only ever handled transiently and is
 * never logged or persisted.
 */
@Configuration
public class PasswordEncoderConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
    }
}
