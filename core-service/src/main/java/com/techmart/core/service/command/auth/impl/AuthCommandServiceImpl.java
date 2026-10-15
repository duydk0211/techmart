package com.techmart.core.service.command.auth.impl;

import com.techmart.core.dto.request.auth.ChangePasswordRequest;
import com.techmart.core.dto.request.auth.LoginRequest;
import com.techmart.core.dto.request.auth.LogoutRequest;
import com.techmart.core.dto.request.auth.RefreshTokenRequest;
import com.techmart.core.dto.request.auth.RegisterRequest;
import com.techmart.core.dto.response.auth.AuthResponse;
import com.techmart.core.dto.response.user.UserProfileResponse;
import com.techmart.core.entity.auth.RefreshTokenEntity;
import com.techmart.core.entity.user.Role;
import com.techmart.core.entity.user.UserEntity;
import com.techmart.core.repository.auth.RefreshTokenRepository;
import com.techmart.core.repository.user.UserRepository;
import com.techmart.core.security.JwtTokenProvider;
import com.techmart.core.service.command.auth.AuthCommandService;
import com.techmart.shared.constant.ResponseCode;
import com.techmart.shared.exception.CoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.UUID;

@Service
@Transactional
public class AuthCommandServiceImpl implements AuthCommandService {

    private static final Logger log = LoggerFactory.getLogger(AuthCommandServiceImpl.class);

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthCommandServiceImpl(UserRepository userRepository,
                                  RefreshTokenRepository refreshTokenRepository,
                                  PasswordEncoder passwordEncoder,
                                  JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    public UserProfileResponse register(RegisterRequest request) {
        log.info("[AuthCommandServiceImpl][register] Registering new customer with email: {}", request.email());

        if (userRepository.existsByEmailAndDeletedFalse(request.email())) {
            log.warn("[AuthCommandServiceImpl][register] Email already registered: {}", request.email());
            throw new CoreException(ResponseCode.EMAIL_ALREADY_EXISTS);
        }

        UserEntity user = new UserEntity();
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFullName(request.fullName());
        user.setPhone(request.phone());
        user.setRole(Role.CUSTOMER);

        UserEntity savedUser = userRepository.save(user);
        return UserProfileResponse.fromEntity(savedUser);
    }

    @Override
    @Transactional(noRollbackFor = CoreException.class)
    public AuthResponse login(LoginRequest request) {
        log.info("[AuthCommandServiceImpl][login] Attempting login for email: {}", request.email());

        UserEntity user = userRepository.findByEmailAndDeletedFalse(request.email())
                .orElseThrow(() -> new CoreException(ResponseCode.INVALID_CREDENTIALS));

        if (user.isAccountLocked()) {
            log.warn("[AuthCommandServiceImpl][login] Account {} is temporarily locked until {}", user.getEmail(), user.getLockedUntil());
            throw new CoreException(ResponseCode.ACCOUNT_TEMPORARILY_LOCKED);
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            user.incrementFailedAttempts();
            userRepository.save(user);
            if (user.isAccountLocked()) {
                log.warn("[AuthCommandServiceImpl][login] Account {} reached 5 failed attempts and is now locked for 15 minutes", user.getEmail());
                throw new CoreException(ResponseCode.ACCOUNT_TEMPORARILY_LOCKED);
            }
            throw new CoreException(ResponseCode.INVALID_CREDENTIALS);
        }

        user.resetFailedAttempts();
        userRepository.save(user);

        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getEmail(), user.getRole().name());
        String rawRefreshToken = UUID.randomUUID().toString();
        String tokenHash = hashSha256(rawRefreshToken);

        RefreshTokenEntity refreshTokenEntity = new RefreshTokenEntity();
        refreshTokenEntity.setUser(user);
        refreshTokenEntity.setTokenHash(tokenHash);
        refreshTokenEntity.setExpiresAt(Instant.now().plus(7, ChronoUnit.DAYS));
        refreshTokenEntity.setRevoked(false);
        refreshTokenRepository.save(refreshTokenEntity);

        return AuthResponse.of(user, accessToken, rawRefreshToken, jwtTokenProvider.getExpirationSeconds());
    }

    @Override
    @Transactional(noRollbackFor = CoreException.class)
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        log.info("[AuthCommandServiceImpl][refreshToken] Refreshing token");
        String tokenHash = hashSha256(request.refreshToken());

        RefreshTokenEntity storedToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new CoreException(ResponseCode.TOKEN_EXPIRED));

        if (storedToken.getExpiresAt().isBefore(Instant.now())) {
            log.warn("[AuthCommandServiceImpl][refreshToken] Token expired for user: {}", storedToken.getUser().getId());
            throw new CoreException(ResponseCode.TOKEN_EXPIRED);
        }

        if (storedToken.isRevoked()) {
            log.error("[AuthCommandServiceImpl][refreshToken] Replay attack detected for user: {}. Revoking all tokens!", storedToken.getUser().getId());
            refreshTokenRepository.revokeAllByUserId(storedToken.getUser().getId());
            throw new CoreException(ResponseCode.TOKEN_REVOKED);
        }

        // Token Rotation: Invalidate current refresh token
        storedToken.setRevoked(true);
        refreshTokenRepository.save(storedToken);

        UserEntity user = storedToken.getUser();
        String newAccessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getEmail(), user.getRole().name());
        String newRawRefreshToken = UUID.randomUUID().toString();
        String newTokenHash = hashSha256(newRawRefreshToken);

        RefreshTokenEntity newTokenEntity = new RefreshTokenEntity();
        newTokenEntity.setUser(user);
        newTokenEntity.setTokenHash(newTokenHash);
        newTokenEntity.setExpiresAt(Instant.now().plus(7, ChronoUnit.DAYS));
        newTokenEntity.setRevoked(false);
        refreshTokenRepository.save(newTokenEntity);

        return AuthResponse.of(user, newAccessToken, newRawRefreshToken, jwtTokenProvider.getExpirationSeconds());
    }

    @Override
    public void logout(LogoutRequest request, UUID userId) {
        log.info("[AuthCommandServiceImpl][logout] Logging out user: {}", userId);
        String tokenHash = hashSha256(request.refreshToken());
        refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
        });
    }

    @Override
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        log.info("[AuthCommandServiceImpl][changePassword] Changing password for user: {}", userId);
        UserEntity user = userRepository.findByIdAndDeletedFalse(userId)
                .orElseThrow(() -> new CoreException(ResponseCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(request.oldPassword(), user.getPasswordHash())) {
            throw new CoreException(ResponseCode.OLD_PASSWORD_INCORRECT);
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.saveAndFlush(user);

        // Revoke all sessions
        refreshTokenRepository.revokeAllByUserId(userId);
    }

    private String hashSha256(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
