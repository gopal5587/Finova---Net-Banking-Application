package com.finova.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;

/**
 * Unit tests for {@link JwtService}. These run without a Spring context so the security
 * logic is exercised in isolation and fast.
 */
class JwtServiceTest {

    // 32+ byte Base64 secret, satisfying the HS256 minimum key length.
    private static final String SECRET = "Zmlub3ZhLXVuaXQtdGVzdC1zZWNyZXQtMDEyMzQ1Njc4OWFiY2RlZg==";

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(new JwtProperties(SECRET, 15, 7));
    }

    @Test
    void accessTokenCarriesSubjectRoleAndType() {
        String token = jwtService.generateAccessToken("alice", "USER");

        Claims claims = jwtService.parse(token);

        assertThat(claims.getSubject()).isEqualTo("alice");
        assertThat(jwtService.extractRole(claims)).isEqualTo("USER");
        assertThat(jwtService.isAccessToken(claims)).isTrue();
        assertThat(jwtService.isRefreshToken(claims)).isFalse();
    }

    @Test
    void refreshTokenIsDistinguishableFromAccessToken() {
        String refresh = jwtService.generateRefreshToken("bob");

        Claims claims = jwtService.parse(refresh);

        assertThat(jwtService.isRefreshToken(claims)).isTrue();
        assertThat(jwtService.isAccessToken(claims)).isFalse();
    }

    @Test
    void tamperedTokenIsRejected() {
        String token = jwtService.generateAccessToken("carol", "USER");
        String tampered = token.substring(0, token.length() - 2) + "xx";

        assertThat(jwtService.isValid(tampered)).isFalse();
        assertThatThrownBy(() -> jwtService.parse(tampered)).isInstanceOf(JwtException.class);
    }

    @Test
    void tokenSignedWithDifferentKeyIsRejected() {
        JwtService other = new JwtService(
                new JwtProperties("b3RoZXItc2VjcmV0LWtleS0wMTIzNDU2Nzg5YWJjZGVmZ2hpams=", 15, 7));
        String foreignToken = other.generateAccessToken("mallory", "ADMIN");

        assertThat(jwtService.isValid(foreignToken)).isFalse();
    }

    @Test
    void weakSecretIsRejectedAtConstruction() {
        assertThatThrownBy(() -> new JwtService(new JwtProperties("short", 15, 7)))
                .isInstanceOf(IllegalStateException.class);
    }
}
