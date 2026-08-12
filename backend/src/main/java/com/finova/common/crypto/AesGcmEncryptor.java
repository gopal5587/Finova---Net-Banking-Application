package com.finova.common.crypto;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * AES-256-GCM encryption for data at rest.
 *
 * <p>GCM is chosen over CBC because it is authenticated: any tampering with the stored
 * ciphertext is detected on decrypt. A fresh random 12-byte IV is generated per value and
 * prepended to the ciphertext, so encrypting the same plaintext twice yields different
 * output (defends against equality inference on encrypted columns).
 *
 * <p>Stored format (Base64 of): {@code [12-byte IV][ciphertext + 16-byte GCM tag]}.
 */
@Component
public class AesGcmEncryptor {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;          // 96-bit IV is the GCM-recommended size
    private static final int GCM_TAG_BITS = 128;      // full-strength authentication tag
    private static final int AES_256_KEY_BYTES = 32;

    private final SecretKeySpec key;
    private final SecureRandom secureRandom = new SecureRandom();

    public AesGcmEncryptor(@Value("${finova.security.aes-secret:${AES_SECRET:}}") String base64Key) {
        byte[] keyBytes = decodeKey(base64Key);
        if (keyBytes.length != AES_256_KEY_BYTES) {
            throw new IllegalStateException(
                    "AES secret must be a Base64-encoded 32-byte (256-bit) key; got " + keyBytes.length + " bytes");
        }
        this.key = new SecretKeySpec(keyBytes, "AES");
    }

    private byte[] decodeKey(String base64Key) {
        if (base64Key == null || base64Key.isBlank()) {
            throw new IllegalStateException("finova.security.aes-secret / AES_SECRET must be configured");
        }
        try {
            return Base64.getDecoder().decode(base64Key.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("AES secret is not valid Base64", e);
        }
    }

    /** Encrypts UTF-8 plaintext, returning Base64(IV || ciphertext||tag). Null in, null out. */
    public String encrypt(String plaintext) {
        if (plaintext == null) {
            return null;
        }
        try {
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            byte[] combined = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);
            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            // Never expose crypto internals; a failure here is a server-side fault.
            throw new IllegalStateException("Failed to encrypt value", e);
        }
    }

    /** Reverses {@link #encrypt(String)}. Throws if the data was tampered with (GCM tag mismatch). */
    public String decrypt(String stored) {
        if (stored == null) {
            return null;
        }
        try {
            byte[] combined = Base64.getDecoder().decode(stored);
            byte[] iv = new byte[IV_LENGTH];
            byte[] ciphertext = new byte[combined.length - IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, IV_LENGTH);
            System.arraycopy(combined, IV_LENGTH, ciphertext, 0, ciphertext.length);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));
            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to decrypt value", e);
        }
    }
}
