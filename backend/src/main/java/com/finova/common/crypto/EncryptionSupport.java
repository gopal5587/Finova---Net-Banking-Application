package com.finova.common.crypto;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Bridges the Spring-managed {@link AesGcmEncryptor} to JPA {@link jakarta.persistence.AttributeConverter}
 * instances, which Hibernate constructs outside the Spring container and therefore cannot autowire.
 *
 * <p>A static reference is populated once at startup. Access is guarded so a converter that runs
 * before the context is ready fails loudly instead of silently persisting plaintext.
 */
@Component
public class EncryptionSupport {

    private static volatile AesGcmEncryptor encryptor;

    @Autowired
    public EncryptionSupport(AesGcmEncryptor encryptor) {
        EncryptionSupport.encryptor = encryptor;
    }

    public static AesGcmEncryptor encryptor() {
        AesGcmEncryptor current = encryptor;
        if (current == null) {
            throw new IllegalStateException("EncryptionSupport not initialised yet");
        }
        return current;
    }
}
