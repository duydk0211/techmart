package com.techmart.core.dto.response.branch;

import com.techmart.core.entity.branch.BranchEntity;

import java.util.UUID;

public record BranchResponse(
        UUID id,
        String name,
        String address,
        String phone,
        int quantityAvailable,
        boolean active
) {
    public static BranchResponse fromEntity(BranchEntity entity) {
        if (entity == null) {
            return null;
        }
        return new BranchResponse(
                entity.getId(),
                entity.getName(),
                entity.getAddress(),
                entity.getPhone(),
                entity.getQuantityAvailable(),
                entity.isActive()
        );
    }
}
