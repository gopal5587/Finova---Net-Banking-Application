package com.finova.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.finova.auth.dto.LoginRequest;
import com.finova.auth.dto.LoginResponse;
import com.finova.auth.dto.MfaCodeRequest;
import com.finova.auth.dto.MfaSetupResponse;
import com.finova.auth.dto.MfaVerifyRequest;
import com.finova.auth.dto.RefreshRequest;
import com.finova.auth.dto.RegisterRequest;
import com.finova.auth.dto.TokenResponse;
import com.finova.auth.dto.UserProfileResponse;
import com.finova.common.exception.BusinessRuleException;
import com.finova.common.exception.DuplicateResourceException;
import com.finova.common.exception.ResourceNotFoundException;
import com.finova.security.JwtService;
import com.finova.security.TotpService;
import com.finova.user.Role;
import com.finova.user.User;
import com.finova.user.UserRepository;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;

/**
 * Orchestrates the authentication lifecycle: registration, credential verification,
 * optional TOTP 2FA, and token refresh.
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final TotpService totpService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtService jwtService,
                       TotpService totpService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.totpService = totpService;
    }

    @Transactional
    public UserProfileResponse register(RegisterRequest request) {
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
     * Verifies username/password. If the user has MFA enabled, returns a short-lived MFA
     * challenge token instead of API tokens.
     */
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));

        User user = userRepository.findByUsernameIgnoreCase(request.username())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.isMfaEnabled()) {
            log.info("User '{}' password ok; MFA challenge required", user.getUsername());
            return LoginResponse.mfaChallenge(jwtService.generateMfaToken(user.getUsername()));
        }

        log.info("User '{}' logged in", user.getUsername());
        TokenResponse tokens = issueTokens(user);
        return LoginResponse.tokens(tokens.accessToken(), tokens.refreshToken());
    }

    @Transactional(readOnly = true)
    public TokenResponse verifyMfa(MfaVerifyRequest request) {
        Claims claims;
        try {
            claims = jwtService.parse(request.mfaToken());
        } catch (JwtException | IllegalArgumentException e) {
            throw new org.springframework.security.authentication.BadCredentialsException("Invalid MFA token");
        }
        if (!jwtService.isMfaToken(claims)) {
            throw new org.springframework.security.authentication.BadCredentialsException("Not an MFA token");
        }

        User user = userRepository.findByUsernameIgnoreCase(claims.getSubject())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (!user.isMfaEnabled() || user.getMfaSecret() == null) {
            throw new BusinessRuleException("MFA is not enabled for this account");
        }
        if (!totpService.verify(user.getMfaSecret(), request.code())) {
            throw new org.springframework.security.authentication.BadCredentialsException("Invalid MFA code");
        }

        log.info("User '{}' completed MFA login", user.getUsername());
        return issueTokens(user);
    }

    @Transactional
    public MfaSetupResponse beginMfaSetup(String username) {
        User user = requireUser(username);
        if (user.isMfaEnabled()) {
            throw new BusinessRuleException("MFA is already enabled");
        }
        String secret = totpService.generateSecret();
        user.setMfaSecret(secret);
        // Not enabled until the user proves they can generate a valid code.
        user.setMfaEnabled(false);
        log.info("Started MFA setup for '{}'", username);
        return new MfaSetupResponse(secret, totpService.qrCodeDataUri(username, secret));
    }

    @Transactional
    public UserProfileResponse enableMfa(String username, MfaCodeRequest request) {
        User user = requireUser(username);
        if (user.getMfaSecret() == null) {
            throw new BusinessRuleException("Call MFA setup first");
        }
        if (user.isMfaEnabled()) {
            throw new BusinessRuleException("MFA is already enabled");
        }
        if (!totpService.verify(user.getMfaSecret(), request.code())) {
            throw new BusinessRuleException("Invalid MFA code; enable aborted");
        }
        user.setMfaEnabled(true);
        log.info("Enabled MFA for '{}'", username);
        return UserProfileResponse.from(user);
    }

    @Transactional
    public UserProfileResponse disableMfa(String username, MfaCodeRequest request) {
        User user = requireUser(username);
        if (!user.isMfaEnabled() || user.getMfaSecret() == null) {
            throw new BusinessRuleException("MFA is not enabled");
        }
        if (!totpService.verify(user.getMfaSecret(), request.code())) {
            throw new BusinessRuleException("Invalid MFA code; disable aborted");
        }
        user.setMfaEnabled(false);
        user.setMfaSecret(null);
        log.info("Disabled MFA for '{}'", username);
        return UserProfileResponse.from(user);
    }

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

    private User requireUser(String username) {
        return userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private TokenResponse issueTokens(User user) {
        String access = jwtService.generateAccessToken(user.getUsername(), user.getRole().name());
        String refresh = jwtService.generateRefreshToken(user.getUsername());
        return TokenResponse.bearer(access, refresh);
    }
}
