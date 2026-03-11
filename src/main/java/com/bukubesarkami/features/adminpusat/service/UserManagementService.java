package com.bukubesarkami.features.adminpusat.service;

import com.bukubesarkami.common.exception.AppException;
import com.bukubesarkami.core.entity.User;
import com.bukubesarkami.core.repository.UserRepository;
import com.bukubesarkami.features.adminpusat.dto.AdminPusatDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserManagementService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public AdminPusatDto.UserResponse createAdminProject(AdminPusatDto.CreateUserRequest request) {
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
                .role(User.Role.ADMIN_PROJECT)
                .active(true)
                .build();

        return toResponse(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public Page<AdminPusatDto.UserResponse> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public AdminPusatDto.UserResponse getUserById(UUID userId) {
        return toResponse(findUser(userId));
    }

    @Transactional
    public AdminPusatDto.UserResponse toggleUserStatus(UUID userId) {
        User user = findUser(userId);
        user.setActive(!user.isActive());
        return toResponse(userRepository.save(user));
    }

    // ===== HELPER =====

    public User findUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new AppException.NotFoundException("User tidak ditemukan: " + userId));
    }

    public AdminPusatDto.UserResponse toResponse(User user) {
        return new AdminPusatDto.UserResponse(
                user.getId(), user.getUsername(), user.getEmail(),
                user.getFullName(), user.getRole().name(), user.isActive(),
                user.getCreatedAt(), user.getUpdatedAt()
        );
    }
}