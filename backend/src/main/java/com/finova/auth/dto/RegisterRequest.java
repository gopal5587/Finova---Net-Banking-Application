package com.finova.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Registration payload. Constraints are enforced at the edge via {@code @Valid} so invalid
 * input never reaches the service layer.
 */
public record RegisterRequest(

        @NotBlank
        @Size(min = 3, max = 50)
        @Pattern(regexp = "^[a-zA-Z0-9_.-]+$", message = "may only contain letters, digits, '.', '_' or '-'")
        String username,

        @NotBlank
        @Email
        @Size(max = 255)
        String email,

        @NotBlank
        @Size(min = 2, max = 150)
        String fullName,

        @NotBlank
        @Size(min = 8, max = 100, message = "must be at least 8 characters")
        String password
) {
}
