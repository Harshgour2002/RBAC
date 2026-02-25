package com.university.auth.dto;

import java.util.Set;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        Set<String> roles,
        Set<String> permissions
) {
}
