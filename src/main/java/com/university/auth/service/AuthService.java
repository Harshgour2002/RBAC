package com.university.auth.service;

import com.university.auth.dto.AuthResponse;
import com.university.auth.dto.LoginRequest;
import com.university.auth.dto.RefreshTokenRequest;
import com.university.auth.dto.SignupRequest;
import com.university.auth.entity.RefreshToken;
import com.university.auth.entity.Role;
import com.university.auth.entity.User;
import com.university.auth.enums.RoleName;
import com.university.auth.exception.ResourceNotFoundException;
import com.university.auth.repository.RoleRepository;
import com.university.auth.repository.UserRepository;
import com.university.auth.security.CustomUserDetails;
import com.university.auth.security.JwtService;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    @Transactional
    public AuthResponse signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email is already registered");
        }

        Role defaultRole = roleRepository.findByName(RoleName.USER.name())
                .orElseThrow(() -> new ResourceNotFoundException("Default USER role not found"));

        User user = User.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .isActive(true)
                .roles(Set.of(defaultRole))
                .build();

        User savedUser = userRepository.save(user);
        CustomUserDetails userDetails = new CustomUserDetails(savedUser);
        String accessToken = jwtService.generateAccessToken(userDetails);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(savedUser);

        log.info("New user registered: {}", request.email());
        return new AuthResponse(
                accessToken,
                refreshToken.getToken(),
                "Bearer",
                jwtService.getAccessTokenExpirationMs(),
                userDetails.getRoles(),
                userDetails.getPermissions());
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        User user = userRepository.findById(userDetails.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        refreshTokenService.deleteByUser(user);
        String accessToken = jwtService.generateAccessToken(userDetails);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        return new AuthResponse(
                accessToken,
                refreshToken.getToken(),
                "Bearer",
                jwtService.getAccessTokenExpirationMs(),
                userDetails.getRoles(),
                userDetails.getPermissions());
    }

    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        RefreshToken rotated = refreshTokenService.rotateRefreshToken(request.refreshToken());
        CustomUserDetails userDetails = new CustomUserDetails(rotated.getUser());
        String accessToken = jwtService.generateAccessToken(userDetails);

        return new AuthResponse(
                accessToken,
                rotated.getToken(),
                "Bearer",
                jwtService.getAccessTokenExpirationMs(),
                userDetails.getRoles(),
                userDetails.getPermissions());
    }
}
