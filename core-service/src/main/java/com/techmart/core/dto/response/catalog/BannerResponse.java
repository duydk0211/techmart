package com.techmart.core.dto.response.catalog;

import com.techmart.core.entity.catalog.BannerEntity;

import java.util.UUID;

public record BannerResponse(
        UUID id,
        String title,
        String imageUrl,
        String targetUrl,
        Integer displayOrder,
        Boolean active
) {
    public static BannerResponse fromEntity(BannerEntity entity) {
        if (entity == null) {
            return null;
        }
        return new BannerResponse(
                entity.getId(),
                entity.getTitle(),
                entity.getImageUrl(),
                entity.getTargetUrl(),
                entity.getDisplayOrder(),
                entity.getActive()
        );
    }
}
