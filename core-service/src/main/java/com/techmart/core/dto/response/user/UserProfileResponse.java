package com.techmart.core.dto.response.user;

import com.techmart.core.entity.user.UserEntity;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

public record UserProfileResponse(
        UUID id,
        String email,
        String fullName,
        String phone,
        String role,
        String avatarUrl,
        int addressesCount,
        Instant createdAt
) implements Serializable {

    public static UserProfileResponse fromEntity(UserEntity user) {
        int addrCount = user.getUserAddressInfo() != null ? user.getUserAddressInfo().size() : 0;
        return new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getPhone(),
                user.getRole().name(),
                user.getAvatarUrl(),
                addrCount,
                user.getCreatedAt()
        );
    }
}
