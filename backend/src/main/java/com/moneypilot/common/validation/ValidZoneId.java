package com.moneypilot.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validates that a string is a valid IANA time-zone id (as recognized by
 * {@link java.time.ZoneId}). A {@code null} value is considered valid so the
 * constraint only applies when a value is supplied; presence is enforced
 * separately where required.
 */
@Documented
@Constraint(validatedBy = ZoneIdValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidZoneId {

    String message() default "must be a valid IANA time-zone id";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
