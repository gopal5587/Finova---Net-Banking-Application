package com.finova.common.crypto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Base64;

import org.junit.jupiter.api.Test;

class AesGcmEncryptorTest {

    // Base64 of a 32-byte key.
    private static final String KEY = Base64.getEncoder()
            .encodeToString("0123456789abcdef0123456789abcdef".getBytes());

    private final AesGcmEncryptor encryptor = new AesGcmEncryptor(KEY);

    @Test
    void roundTripReturnsOriginalPlaintext() {
        String plaintext = "ABCDE1234F";

        String encrypted = encryptor.encrypt(plaintext);

        assertThat(encrypted).isNotNull().isNotEqualTo(plaintext);
        assertThat(encryptor.decrypt(encrypted)).isEqualTo(plaintext);
    }

    @Test
    void encryptingSameValueTwiceProducesDifferentCiphertext() {
        // Random per-value IV means ciphertexts differ, preventing equality inference.
        String a = encryptor.encrypt("same-value");
        String b = encryptor.encrypt("same-value");

        assertThat(a).isNotEqualTo(b);
        assertThat(encryptor.decrypt(a)).isEqualTo(encryptor.decrypt(b));
    }

    @Test
    void nullIsPassedThrough() {
        assertThat(encryptor.encrypt(null)).isNull();
        assertThat(encryptor.decrypt(null)).isNull();
    }

    @Test
    void tamperedCiphertextIsRejected() {
        String encrypted = encryptor.encrypt("secret");
        byte[] raw = Base64.getDecoder().decode(encrypted);
        raw[raw.length - 1] ^= 0x01; // flip a bit in the GCM tag
        String tampered = Base64.getEncoder().encodeToString(raw);

        assertThatThrownBy(() -> encryptor.decrypt(tampered)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void wrongSizedKeyIsRejected() {
        String shortKey = Base64.getEncoder().encodeToString("too-short-key".getBytes());
        assertThatThrownBy(() -> new AesGcmEncryptor(shortKey)).isInstanceOf(IllegalStateException.class);
    }
}
