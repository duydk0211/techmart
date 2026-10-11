package com.techmart.core.dto.response.catalog;

import com.techmart.core.entity.catalog.BrandEntity;

import java.util.UUID;

public record BrandResponse(
        UUID id,
        String name,
        String description,
        String logoUrl,
        boolean active
) {
    public static BrandResponse fromEntity(BrandEntity entity) {
        if (entity == null) {
            return null;
        }
        return new BrandResponse(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getLogoUrl(),
                entity.isActive()
        );
    }
}
