package com.finova.auth.dto;

/**
 * Token pair returned on successful login/refresh.
 *
 * @param tokenType always "Bearer" for convenience of clients building the Authorization header
 */
public record TokenResponse(
        String accessToken,
        String refreshToken,
        String tokenType
) {
    public static TokenResponse bearer(String accessToken, String refreshToken) {
        return new TokenResponse(accessToken, refreshToken, "Bearer");
    }
}
