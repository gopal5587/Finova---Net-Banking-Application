package com.finova.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record MfaCodeRequest(
        @NotBlank
        @Pattern(regexp = "^\\d{6}$", message = "must be a 6-digit code")
        String code
) {
}
