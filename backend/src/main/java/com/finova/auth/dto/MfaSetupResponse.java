package com.finova.auth.dto;

/** Returned when a user starts 2FA enrollment. The secret is shown once for authenticator apps. */
public record MfaSetupResponse(
        String secret,
        String qrCodeDataUri
) {
}
