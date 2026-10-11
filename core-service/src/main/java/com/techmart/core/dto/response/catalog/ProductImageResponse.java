package com.techmart.core.dto.response.catalog;

import com.techmart.core.entity.catalog.ProductImageEntity;

import java.util.UUID;

public record ProductImageResponse(
        UUID id,
        String imageUrl,
        boolean isPrimary,
        int displayOrder
) {
    public static ProductImageResponse fromEntity(ProductImageEntity entity) {
        if (entity == null) {
            return null;
        }
        return new ProductImageResponse(
                entity.getId(),
                entity.getImageUrl(),
                entity.isPrimary(),
                entity.getDisplayOrder()
        );
    }
}
