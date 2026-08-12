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
import com.finova.auth.dto.LoginResponse;
import com.finova.auth.dto.MfaCodeRequest;
import com.finova.auth.dto.MfaSetupResponse;
import com.finova.auth.dto.MfaVerifyRequest;
import com.finova.auth.dto.RefreshRequest;
import com.finova.auth.dto.RegisterRequest;
import com.finova.auth.dto.TokenResponse;
import com.finova.auth.dto.UserProfileResponse;
import com.finova.common.exception.ResourceNotFoundException;
import com.finova.user.UserRepository;

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
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/mfa/verify")
    public ResponseEntity<TokenResponse> verifyMfa(@Valid @RequestBody MfaVerifyRequest request) {
        return ResponseEntity.ok(authService.verifyMfa(request));
    }

    @PostMapping("/mfa/setup")
    public ResponseEntity<MfaSetupResponse> setupMfa(Authentication auth) {
        return ResponseEntity.ok(authService.beginMfaSetup(auth.getName()));
    }

    @PostMapping("/mfa/enable")
    public ResponseEntity<UserProfileResponse> enableMfa(Authentication auth,
                                                         @Valid @RequestBody MfaCodeRequest request) {
        return ResponseEntity.ok(authService.enableMfa(auth.getName(), request));
    }

    @PostMapping("/mfa/disable")
    public ResponseEntity<UserProfileResponse> disableMfa(Authentication auth,
                                                          @Valid @RequestBody MfaCodeRequest request) {
        return ResponseEntity.ok(authService.disableMfa(auth.getName(), request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> me(Authentication authentication) {
        String username = authentication.getName();
        UserProfileResponse profile = userRepository.findByUsernameIgnoreCase(username)
                .map(UserProfileResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return ResponseEntity.ok(profile);
    }
}
