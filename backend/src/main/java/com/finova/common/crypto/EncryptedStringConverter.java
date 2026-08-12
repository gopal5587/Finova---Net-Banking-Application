package com.finova.common.crypto;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Transparently encrypts a String column at rest. Apply with
 * {@code @Convert(converter = EncryptedStringConverter.class)} on sensitive fields.
 *
 * <p>Not marked {@code autoApply} so encryption is an explicit, auditable decision per field.
 */
@Converter
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    @Override
    public String convertToDatabaseColumn(String attribute) {
        return EncryptionSupport.encryptor().encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        return EncryptionSupport.encryptor().decrypt(dbData);
    }
}
