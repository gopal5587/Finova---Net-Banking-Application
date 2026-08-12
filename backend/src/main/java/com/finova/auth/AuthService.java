package com.finova.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.finova.auth.dto.LoginRequest;
import com.finova.auth.dto.RefreshRequest;
import com.finova.auth.dto.RegisterRequest;
import com.finova.auth.dto.TokenResponse;
import com.finova.auth.dto.UserProfileResponse;
import com.finova.common.exception.DuplicateResourceException;
import com.finova.common.exception.ResourceNotFoundException;
import com.finova.security.JwtService;
import com.finova.user.Role;
import com.finova.user.User;
import com.finova.user.UserRepository;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;

/**
 * Orchestrates the authentication lifecycle: registration, credential verification, and
 * token refresh. All persistence happens inside transactions so a partial write can never
 * leave a half-created account.
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @Transactional
    public UserProfileResponse register(RegisterRequest request) {
        // Pre-check for friendlier errors; the DB unique indexes remain the source of truth.
        if (userRepository.existsByUsernameIgnoreCase(request.username())) {
            throw new DuplicateResourceException("Username is already taken");
        }
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new DuplicateResourceException("Email is already registered");
        }

        User user = new User();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setFullName(request.fullName());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(Role.USER);
        user.setEnabled(true);

        User saved = userRepository.save(user);
        log.info("Registered new user '{}' ({})", saved.getUsername(), saved.getPublicId());
        return UserProfileResponse.from(saved);
    }

    /**
     * Verifies credentials via the {@link AuthenticationManager} (which triggers BCrypt matching)
     * and issues a fresh token pair. Never reveals whether the username or password was wrong.
     */
    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));

        User user = userRepository.findByUsernameIgnoreCase(request.username())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        log.info("User '{}' logged in", user.getUsername());
        return issueTokens(user);
    }

    /** Exchanges a valid refresh token for a new token pair (rotation). */
    @Transactional(readOnly = true)
    public TokenResponse refresh(RefreshRequest request) {
        final Claims claims;
        try {
            claims = jwtService.parse(request.refreshToken());
        } catch (JwtException | IllegalArgumentException e) {
            throw new org.springframework.security.authentication.BadCredentialsException("Invalid refresh token");
        }
        if (!jwtService.isRefreshToken(claims)) {
            throw new org.springframework.security.authentication.BadCredentialsException("Not a refresh token");
        }

        User user = userRepository.findByUsernameIgnoreCase(claims.getSubject())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return issueTokens(user);
    }

    private TokenResponse issueTokens(User user) {
        String access = jwtService.generateAccessToken(user.getUsername(), user.getRole().name());
        String refresh = jwtService.generateRefreshToken(user.getUsername());
        return TokenResponse.bearer(access, refresh);
    }
}
