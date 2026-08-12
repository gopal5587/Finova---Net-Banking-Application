package com.finova.security;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

import javax.crypto.SecretKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.WeakKeyException;

/**
 * Issues and validates JWTs (HS256).
 *
 * <p>Two token types are produced: short-lived <em>access</em> tokens for API calls and
 * longer-lived <em>refresh</em> tokens used only to mint new access tokens. The token
 * {@code type} claim prevents a refresh token from being replayed as an access token.
 */
@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    private static final String CLAIM_TYPE = "type";
    private static final String CLAIM_ROLE = "role";
    private static final String TYPE_ACCESS = "access";
    private static final String TYPE_REFRESH = "refresh";

    private final SecretKey signingKey;
    private final Duration accessTtl;
    private final Duration refreshTtl;

    public JwtService(JwtProperties props) {
        // Fail fast on a weak/misconfigured key rather than silently issuing forgeable tokens.
        this.signingKey = buildKey(props.secret());
        this.accessTtl = Duration.ofMinutes(props.accessTokenTtlMinutes());
        this.refreshTtl = Duration.ofDays(props.refreshTokenTtlDays());
    }

    private SecretKey buildKey(String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("finova.security.jwt.secret must be configured");
        }
        byte[] keyBytes;
        try {
            // Prefer Base64; fall back to raw bytes so operators can supply either form.
            keyBytes = Decoders.BASE64.decode(secret);
        } catch (IllegalArgumentException notBase64) {
            keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        }
        try {
            return Keys.hmacShaKeyFor(keyBytes);
        } catch (WeakKeyException e) {
            throw new IllegalStateException("JWT secret is too short; provide at least 256 bits (32 bytes)", e);
        }
    }

    public String generateAccessToken(String subject, String role) {
        return build(subject, TYPE_ACCESS, Map.of(CLAIM_ROLE, role), accessTtl);
    }

    public String generateRefreshToken(String subject) {
        return build(subject, TYPE_REFRESH, Map.of(), refreshTtl);
    }

    private String build(String subject, String type, Map<String, ?> extraClaims, Duration ttl) {
        Instant now = Instant.now();
        var builder = Jwts.builder()
                .subject(subject)
                .claim(CLAIM_TYPE, type)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttl)));
        extraClaims.forEach(builder::claim);
        return builder.signWith(signingKey).compact();
    }

    /** Parses and verifies a token, returning its claims, or throws {@link JwtException} if invalid/expired. */
    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isAccessToken(Claims claims) {
        return TYPE_ACCESS.equals(claims.get(CLAIM_TYPE, String.class));
    }

    public boolean isRefreshToken(Claims claims) {
        return TYPE_REFRESH.equals(claims.get(CLAIM_TYPE, String.class));
    }

    public String extractRole(Claims claims) {
        return claims.get(CLAIM_ROLE, String.class);
    }

    /** Null-safe validation helper for callers that prefer a boolean over exception handling. */
    public boolean isValid(String token) {
        try {
            parse(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Rejected token: {}", e.getMessage());
            return false;
        }
    }
}
