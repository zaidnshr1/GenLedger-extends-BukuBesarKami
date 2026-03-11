package com.bukubesarkami.common.util;

import com.bukubesarkami.core.entity.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SecurityUtil {

    public User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new IllegalStateException("No authenticated user found");
        }
        return (User) auth.getPrincipal();
    }

    public String getCurrentUsername() {
        return getCurrentUser().getUsername();
    }
}