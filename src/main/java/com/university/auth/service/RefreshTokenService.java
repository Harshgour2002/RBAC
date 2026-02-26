package com.university.auth.service;

import com.university.auth.entity.RefreshToken;
import com.university.auth.entity.User;
import com.university.auth.exception.ResourceNotFoundException;
import com.university.auth.exception.TokenExpiredException;
import com.university.auth.repository.RefreshTokenRepository;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${security.jwt.refresh-token-expiration-ms}")
    private long refreshTokenExpirationMs;

    @Transactional
    public RefreshToken createRefreshToken(User user) {
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .expiryDate(LocalDateTime.now().plusNanos(refreshTokenExpirationMs * 1_000_000))
                .build();
        return refreshTokenRepository.save(refreshToken);
    }

    public RefreshToken verifyExpiration(String token) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Refresh token not found"));

        if (refreshToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            refreshTokenRepository.delete(refreshToken);
            throw new TokenExpiredException("Refresh token expired");
        }

        return refreshToken;
    }

    @Transactional
    public void deleteByUser(User user) {
        refreshTokenRepository.deleteByUser(user);
    }

    @Transactional
    public RefreshToken rotateRefreshToken(String token) {
        RefreshToken existing = verifyExpiration(token);
        User user = existing.getUser();
        refreshTokenRepository.delete(existing);
        return createRefreshToken(user);
    }
}
