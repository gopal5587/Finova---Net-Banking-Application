package com.finova.account;

import java.security.SecureRandom;

import org.springframework.stereotype.Component;

/**
 * Generates account numbers using a cryptographically-strong RNG.
 *
 * <p>{@link SecureRandom} (not {@link java.util.Random}) is used deliberately: predictable
 * account numbers would let an attacker enumerate accounts. The format is a fixed "FN" prefix
 * plus 12 random digits. Uniqueness is still enforced by the DB index; callers should retry on
 * the astronomically unlikely collision.
 */
@Component
public class AccountNumberGenerator {

    private static final String PREFIX = "FN";
    private static final int DIGITS = 12;

    private final SecureRandom secureRandom = new SecureRandom();

    public String generate() {
        StringBuilder sb = new StringBuilder(PREFIX);
        for (int i = 0; i < DIGITS; i++) {
            sb.append(secureRandom.nextInt(10));
        }
        return sb.toString();
    }
}
