package com.finova.auth;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.finova.auth.dto.LoginRequest;
import com.finova.auth.dto.RefreshRequest;
import com.finova.auth.dto.RegisterRequest;
import com.finova.auth.dto.TokenResponse;
import com.finova.auth.dto.UserProfileResponse;
import com.finova.common.exception.ResourceNotFoundException;
import com.finova.user.UserRepository;

/**
 * Public authentication endpoints plus a protected {@code /me} for the current user.
 * Business logic lives in {@link AuthService}; the controller only handles HTTP concerns.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;

    public AuthController(AuthService authService, UserRepository userRepository) {
        this.authService = authService;
        this.userRepository = userRepository;
    }

    @PostMapping("/register")
    public ResponseEntity<UserProfileResponse> register(@Valid @RequestBody RegisterRequest request) {
        UserProfileResponse profile = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(profile);
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }

    /** Returns the authenticated caller's profile; the username comes from the validated JWT. */
    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> me(Authentication authentication) {
        String username = authentication.getName();
        UserProfileResponse profile = userRepository.findByUsernameIgnoreCase(username)
                .map(UserProfileResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return ResponseEntity.ok(profile);
    }
}
