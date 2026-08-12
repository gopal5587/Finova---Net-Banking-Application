package com.finova.auth.dto;

import java.util.UUID;

import com.finova.user.User;

/** Safe, read-only view of a user. Never exposes the password hash, internal id, or MFA secret. */
public record UserProfileResponse(
        UUID id,
        String username,
        String email,
        String fullName,
        String role,
        boolean mfaEnabled
) {
    public static UserProfileResponse from(User user) {
        return new UserProfileResponse(
                user.getPublicId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getRole().name(),
                user.isMfaEnabled()
        );
    }
}
