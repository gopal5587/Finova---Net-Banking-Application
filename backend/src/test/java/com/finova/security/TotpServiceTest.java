package com.finova.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.exceptions.CodeGenerationException;
import dev.samstevens.totp.time.SystemTimeProvider;

class TotpServiceTest {

    private final TotpService totpService = new TotpService();

    @Test
    void generatedSecretProducesVerifiableCode() throws CodeGenerationException {
        String secret = totpService.generateSecret();
        assertThat(secret).isNotBlank();

        long counter = new SystemTimeProvider().getTime() / 30;
        String code = new DefaultCodeGenerator(HashingAlgorithm.SHA1).generate(secret, counter);

        assertThat(totpService.verify(secret, code)).isTrue();
        assertThat(totpService.verify(secret, "000000")).isFalse();
    }

    @Test
    void qrCodeDataUriIsPngDataUrl() {
        String secret = totpService.generateSecret();
        String uri = totpService.qrCodeDataUri("alice", secret);
        assertThat(uri).startsWith("data:image/png;base64,");
    }
}
