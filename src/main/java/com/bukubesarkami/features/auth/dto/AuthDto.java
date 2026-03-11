package com.bukubesarkami.features.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.UUID;

public final class AuthDto {

    private AuthDto() {}

    public record RegisterRequest(
            @NotBlank @Size(min = 3, max = 50)
            String username,

            @NotBlank @Email
            String email,

            @NotBlank
            @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&]).{8,}$",
                    message = "Password minimal 8 karakter, harus mengandung huruf besar, kecil, angka, dan karakter spesial")
            String password,

            @NotBlank @Size(min = 2, max = 100)
            String fullName
    ) {}

    public record LoginRequest(
            @NotBlank String username,
            @NotBlank String password
    ) {}

    public record RefreshTokenRequest(
            @NotBlank String refreshToken
    ) {}

    public record TokenResponse(
            String accessToken,
            String refreshToken,
            String tokenType,
            long expiresIn,
            UUID userId,
            String username,
            String role,
            OffsetDateTime issuedAt
    ) {}

    public record UserInfo(
            UUID id,
            String username,
            String email,
            String fullName,
            String role,
            boolean active,
            OffsetDateTime createdAt
    ) {}
}