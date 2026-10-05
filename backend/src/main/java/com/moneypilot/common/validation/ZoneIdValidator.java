package com.moneypilot.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.ZoneId;

/**
 * {@link ValidZoneId} constraint validator. Treats {@code null} as valid (the
 * value is optional); a supplied value must be a zone id known to
 * {@link ZoneId#getAvailableZoneIds()}.
 */
public class ZoneIdValidator implements ConstraintValidator<ValidZoneId, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        return ZoneId.getAvailableZoneIds().contains(value);
    }
}
