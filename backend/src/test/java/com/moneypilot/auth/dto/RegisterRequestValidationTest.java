package com.moneypilot.auth.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class RegisterRequestValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    private Set<String> invalidFields(RegisterRequest request) {
        return validator.validate(request).stream()
                .map(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .collect(Collectors.toSet());
    }

    @Test
    void validRequestWithOptionalFieldsOmittedHasNoViolations() {
        RegisterRequest request =
                new RegisterRequest("ada@example.com", "password1", "Ada", null, null);
        assertThat(invalidFields(request)).isEmpty();
    }

    @Test
    void validRequestWithSuppliedCurrencyAndTimezoneHasNoViolations() {
        RegisterRequest request =
                new RegisterRequest("ada@example.com", "password1", "Ada", "USD", "America/New_York");
        assertThat(invalidFields(request)).isEmpty();
    }

    @Test
    void blankRequiredFieldsAreRejected() {
        RegisterRequest request = new RegisterRequest("", "", "", null, null);
        assertThat(invalidFields(request)).contains("email", "password", "displayName");
    }

    @Test
    void passwordShorterThanEightIsRejected() {
        RegisterRequest request =
                new RegisterRequest("ada@example.com", "short7!", "Ada", null, null);
        assertThat(invalidFields(request)).contains("password");
    }

    @Test
    void passwordOf128IsAcceptedAndNotTruncated() {
        String pwd = "a".repeat(128);
        RegisterRequest request = new RegisterRequest("ada@example.com", pwd, "Ada", null, null);
        assertThat(invalidFields(request)).doesNotContain("password");
    }

    @Test
    void passwordLongerThan128IsRejected() {
        String pwd = "a".repeat(129);
        RegisterRequest request = new RegisterRequest("ada@example.com", pwd, "Ada", null, null);
        assertThat(invalidFields(request)).contains("password");
    }

    @Test
    void malformedEmailIsRejected() {
        RegisterRequest request =
                new RegisterRequest("not-an-email", "password1", "Ada", null, null);
        assertThat(invalidFields(request)).contains("email");
    }

    @Test
    void emailLongerThan254IsRejected() {
        String longLocal = "a".repeat(250);
        String email = longLocal + "@e.com"; // > 254 chars
        RegisterRequest request = new RegisterRequest(email, "password1", "Ada", null, null);
        assertThat(invalidFields(request)).contains("email");
    }

    @Test
    void displayNameLongerThan100IsRejected() {
        String name = "n".repeat(101);
        RegisterRequest request =
                new RegisterRequest("ada@example.com", "password1", name, null, null);
        assertThat(invalidFields(request)).contains("displayName");
    }

    @Test
    void lowercaseCurrencyIsRejected() {
        RegisterRequest request =
                new RegisterRequest("ada@example.com", "password1", "Ada", "inr", null);
        assertThat(invalidFields(request)).contains("currency");
    }

    @Test
    void currencyNotThreeLettersIsRejected() {
        RegisterRequest request =
                new RegisterRequest("ada@example.com", "password1", "Ada", "INRX", null);
        assertThat(invalidFields(request)).contains("currency");
    }

    @Test
    void invalidTimezoneIsRejected() {
        RegisterRequest request =
                new RegisterRequest("ada@example.com", "password1", "Ada", null, "Mars/Phobos");
        assertThat(invalidFields(request)).contains("timezone");
    }
}
