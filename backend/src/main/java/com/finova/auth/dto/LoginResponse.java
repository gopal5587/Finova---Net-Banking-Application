package com.finova.auth.dto;

/**
 * Login outcome. Either a full token pair, or an MFA challenge that must be completed via
 * {@code /auth/mfa/verify} before tokens are issued.
 */
public record LoginResponse(
        boolean mfaRequired,
        String mfaToken,
        String accessToken,
        String refreshToken,
        String tokenType
) {
    public static LoginResponse tokens(String accessToken, String refreshToken) {
        return new LoginResponse(false, null, accessToken, refreshToken, "Bearer");
    }

    public static LoginResponse mfaChallenge(String mfaToken) {
        return new LoginResponse(true, mfaToken, null, null, null);
    }
}
