package com.moneypilot.common;

/**
 * Raised for a request that is well-formed but fails a cross-field business
 * rule that bean validation cannot express on its own (e.g. a statement due
 * date before its statement date). Maps to {@code 400 BAD_REQUEST} with the
 * provided message.
 */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
