package com.finova.transaction;

import java.security.SecureRandom;
import java.time.Instant;

import org.springframework.stereotype.Component;

/**
 * Produces human-readable, hard-to-guess transaction references of the form
 * {@code TXN-<epochMillis>-<6 random alphanumerics>}. The random suffix (from {@link SecureRandom})
 * keeps references unpredictable while the timestamp keeps them roughly sortable.
 */
@Component
public class TransactionReferenceGenerator {

    private static final char[] ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();
    private static final int SUFFIX_LENGTH = 6;

    private final SecureRandom secureRandom = new SecureRandom();

    public String generate() {
        StringBuilder suffix = new StringBuilder(SUFFIX_LENGTH);
        for (int i = 0; i < SUFFIX_LENGTH; i++) {
            suffix.append(ALPHABET[secureRandom.nextInt(ALPHABET.length)]);
        }
        return "TXN-" + Instant.now().toEpochMilli() + "-" + suffix;
    }
}
