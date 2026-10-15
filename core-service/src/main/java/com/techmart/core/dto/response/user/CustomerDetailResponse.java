package com.techmart.core.dto.response.user;

import com.techmart.core.entity.user.UserEntity;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CustomerDetailResponse(
        UUID id,
        String email,
        String fullName,
        String phone,
        String role,
        String avatarUrl,
        List<AddressResponse> addresses,
        int failedLoginAttempts,
        Instant lockedUntil,
        boolean deleted,
        Instant createdAt,
        Instant updatedAt
) implements Serializable {

    public static CustomerDetailResponse fromEntity(UserEntity user) {
        List<AddressResponse> addrList = user.getUserAddressInfo() != null
                ? user.getUserAddressInfo().stream().map(AddressResponse::fromAddressItem).toList()
                : List.of();
        return new CustomerDetailResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getPhone(),
                user.getRole().name(),
                user.getAvatarUrl(),
                addrList,
                user.getFailedLoginAttempts(),
                user.getLockedUntil(),
                user.isDeleted(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
