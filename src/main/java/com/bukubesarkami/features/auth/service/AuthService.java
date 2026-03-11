package com.bukubesarkami.features.auth.service;

import com.bukubesarkami.common.exception.AppException;
import com.bukubesarkami.config.JwtProperties;
import com.bukubesarkami.config.JwtService;
import com.bukubesarkami.config.RateLimiterService;
import com.bukubesarkami.core.entity.RefreshToken;
import com.bukubesarkami.core.entity.User;
import com.bukubesarkami.core.repository.RefreshTokenRepository;
import com.bukubesarkami.core.repository.UserRepository;
import com.bukubesarkami.features.auth.dto.AuthDto;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final RateLimiterService rateLimiterService;

    @Transactional
    public AuthDto.UserInfo registerAdminPusat(AuthDto.RegisterRequest request) {
        if (userRepository.existsByUsername(request.username().trim().toLowerCase())) {
            throw new AppException.DuplicateException("Username sudah digunakan.");
        }
        if (userRepository.existsByEmail(request.email().trim().toLowerCase())) {
            throw new AppException.DuplicateException("Email sudah digunakan.");
        }

        User user = User.builder()
                .username(request.username().trim().toLowerCase())
                .email(request.email().trim().toLowerCase())
                .password(passwordEncoder.encode(request.password()))
                .fullName(request.fullName().trim())
                .role(User.Role.ADMIN_PUSAT)
                .active(true)
                .build();

        return toUserInfo(userRepository.save(user));
    }

    /**
     * Login dengan rate limiting per IP.
     * Menyimpan metadata device (User-Agent, IP) pada refresh token.
     */
    @Transactional
    public AuthDto.TokenResponse login(AuthDto.LoginRequest request, String ipAddress, String userAgent) {
        // Rate limit check — lempar TooManyRequestsException jika melebihi batas
        rateLimiterService.checkLoginRateLimit(ipAddress);

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );

        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new AppException.NotFoundException("User tidak ditemukan."));

        if (!user.isActive()) {
            throw new AppException.BusinessException("Akun tidak aktif.");
        }

        // Revoke semua refresh token lama (single session per user)
        refreshTokenRepository.revokeAllByUserId(user.getId());

        String accessToken = jwtService.generateAccessToken(user);
        RefreshToken refreshToken = createRefreshToken(user, ipAddress, userAgent);

        return buildTokenResponse(user, accessToken, refreshToken.getToken());
    }

    @Transactional
    public AuthDto.TokenResponse refreshToken(AuthDto.RefreshTokenRequest request,
                                              String ipAddress, String userAgent) {
        RefreshToken stored = refreshTokenRepository.findByToken(request.refreshToken())
                .orElseThrow(() -> new AppException.InvalidTokenException("Refresh token tidak ditemukan."));

        if (!stored.isValid()) {
            // Token tidak valid → kemungkinan token theft, revoke semua
            refreshTokenRepository.revokeAllByUserId(stored.getUser().getId());
            throw new AppException.InvalidTokenException("Refresh token tidak valid. Semua sesi telah diakhiri.");
        }

        // Deteksi IP berbeda — log peringatan (opsional: bisa tolak)
        if (stored.getIpAddress() != null && !stored.getIpAddress().equals(ipAddress)) {
            // Di produksi bisa kirim notifikasi email
            // Untuk sekarang tetap izinkan tapi log warning
        }

        // Rotate refresh token
        stored.setRevoked(true);
        refreshTokenRepository.save(stored);

        User user = stored.getUser();
        String newAccessToken = jwtService.generateAccessToken(user);
        RefreshToken newRefreshToken = createRefreshToken(user, ipAddress, userAgent);

        return buildTokenResponse(user, newAccessToken, newRefreshToken.getToken());
    }

    @Transactional
    public void logout(UUID userId) {
        refreshTokenRepository.revokeAllByUserId(userId);
    }

    public AuthDto.UserInfo getProfile(User user) {
        return toUserInfo(user);
    }

    // ===== PRIVATE HELPERS =====

    private RefreshToken createRefreshToken(User user, String ipAddress, String userAgent) {
        String deviceInfo = extractDeviceInfo(userAgent);
        RefreshToken token = RefreshToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .expiresAt(OffsetDateTime.now().plusSeconds(jwtProperties.getRefreshExpirationMs() / 1000))
                .revoked(false)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .deviceInfo(deviceInfo)
                .build();
        return refreshTokenRepository.save(token);
    }

    private String extractDeviceInfo(String userAgent) {
        if (userAgent == null) return "Unknown";
        if (userAgent.contains("Mobile"))  return "Mobile Browser";
        if (userAgent.contains("Chrome"))  return "Chrome Browser";
        if (userAgent.contains("Firefox")) return "Firefox Browser";
        if (userAgent.contains("Safari"))  return "Safari Browser";
        if (userAgent.contains("Postman")) return "Postman Client";
        return "Browser/App";
    }

    private AuthDto.TokenResponse buildTokenResponse(User user, String accessToken, String refreshToken) {
        return new AuthDto.TokenResponse(
                accessToken, refreshToken, "Bearer",
                jwtProperties.getExpirationMs() / 1000,
                user.getId(), user.getUsername(), user.getRole().name(),
                OffsetDateTime.now()
        );
    }

    private AuthDto.UserInfo toUserInfo(User user) {
        return new AuthDto.UserInfo(
                user.getId(), user.getUsername(), user.getEmail(),
                user.getFullName(), user.getRole().name(),
                user.isActive(), user.getCreatedAt()
        );
    }
}