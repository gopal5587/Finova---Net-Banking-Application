package com.finova.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Strongly-typed JWT settings bound from {@code finova.security.jwt.*}.
 * Using typed properties (instead of scattered {@code @Value}) means a missing or
 * malformed setting fails fast at startup rather than at first request.
 *
 * @param secret               Base64-encoded signing key (must decode to >= 256 bits for HS256)
 * @param accessTokenTtlMinutes access-token lifetime in minutes
 * @param refreshTokenTtlDays   refresh-token lifetime in days
 */
@ConfigurationProperties(prefix = "finova.security.jwt")
public record JwtProperties(
        String secret,
        long accessTokenTtlMinutes,
        long refreshTokenTtlDays
) {
}
