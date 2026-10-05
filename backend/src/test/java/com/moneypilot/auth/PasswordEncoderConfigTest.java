package com.moneypilot.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

class PasswordEncoderConfigTest {

    private final PasswordEncoder encoder = new PasswordEncoderConfig().passwordEncoder();

    @Test
    void encodesToArgon2idHash() {
        String encoded = encoder.encode("password1");
        assertThat(encoded).startsWith("$argon2id$");
    }

    @Test
    void matchesCorrectPassword() {
        String encoded = encoder.encode("correct horse battery");
        assertThat(encoder.matches("correct horse battery", encoded)).isTrue();
    }

    @Test
    void doesNotMatchWrongPassword() {
        String encoded = encoder.encode("correct horse battery");
        assertThat(encoder.matches("wrong password", encoded)).isFalse();
    }

    @Test
    void samePasswordEncodesDifferentlyDueToRandomSalt() {
        String a = encoder.encode("password1");
        String b = encoder.encode("password1");
        assertThat(a).isNotEqualTo(b);
        assertThat(encoder.matches("password1", a)).isTrue();
        assertThat(encoder.matches("password1", b)).isTrue();
    }

    @Test
    void longPasswordIsNotTruncated() {
        // A 128-char password and the same password differing only at char 100
        // must NOT be treated as equal (would happen if input were truncated).
        String base = "a".repeat(128);
        String differsAt100 = "a".repeat(99) + "X" + "a".repeat(28); // length 128
        assertThat(differsAt100).hasSize(128);

        String encodedBase = encoder.encode(base);
        assertThat(encoder.matches(base, encodedBase)).isTrue();
        assertThat(encoder.matches(differsAt100, encodedBase)).isFalse();
    }
}
