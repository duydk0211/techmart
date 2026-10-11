package com.techmart.core.dto.response.auth;

import com.techmart.core.entity.user.UserEntity;

import java.io.Serializable;

public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        String refreshToken,
        UserSummaryResponse user
) implements Serializable {

    public static AuthResponse of(UserEntity user, String accessToken, String refreshToken, long expiresIn) {
        UserSummaryResponse summary = new UserSummaryResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole().name()
        );
        return new AuthResponse(accessToken, "Bearer", expiresIn, refreshToken, summary);
    }
}
