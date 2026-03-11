package com.bukubesarkami.features.auth.controller;

import com.bukubesarkami.common.util.ApiResponse;
import com.bukubesarkami.common.util.SecurityUtil;
import com.bukubesarkami.features.auth.dto.AuthDto;
import com.bukubesarkami.features.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Autentikasi dan manajemen sesi")
public class AuthController {

    private final AuthService authService;
    private final SecurityUtil securityUtil;

    @PostMapping("/register")
    @Operation(summary = "Register Admin Pusat")
    public ResponseEntity<ApiResponse<AuthDto.UserInfo>> register(
            @Valid @RequestBody AuthDto.RegisterRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Registrasi berhasil", authService.registerAdminPusat(request)));
    }

    @PostMapping("/login")
    @Operation(summary = "Login — dibatasi 5x per 60 detik per IP")
    public ResponseEntity<ApiResponse<AuthDto.TokenResponse>> login(
            @Valid @RequestBody AuthDto.LoginRequest request,
            HttpServletRequest httpRequest) {

        String ip = extractClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        return ResponseEntity.ok(ApiResponse.ok("Login berhasil", authService.login(request, ip, userAgent)));
    }

    @PostMapping("/refresh-token")
    @Operation(summary = "Perbarui access token (refresh token rotation)")
    public ResponseEntity<ApiResponse<AuthDto.TokenResponse>> refreshToken(
            @Valid @RequestBody AuthDto.RefreshTokenRequest request,
            HttpServletRequest httpRequest) {

        String ip = extractClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        return ResponseEntity.ok(ApiResponse.ok("Token diperbarui",
                authService.refreshToken(request, ip, userAgent)));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout dan revoke semua token", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<Void>> logout() {
        authService.logout(securityUtil.getCurrentUser().getId());
        return ResponseEntity.ok(ApiResponse.ok("Logout berhasil"));
    }

    @GetMapping("/me")
    @Operation(summary = "Informasi akun saat ini", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<AuthDto.UserInfo>> me() {
        return ResponseEntity.ok(ApiResponse.ok(authService.getProfile(securityUtil.getCurrentUser())));
    }

    // ===== HELPER =====
    private String extractClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) return xff.split(",")[0].trim();
        return request.getRemoteAddr();
    }
}