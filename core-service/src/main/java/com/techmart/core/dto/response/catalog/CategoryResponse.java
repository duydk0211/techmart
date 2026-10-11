package com.techmart.core.dto.response.catalog;

import com.techmart.core.entity.catalog.CategoryEntity;

import java.time.Instant;
import java.util.UUID;

public record CategoryResponse(
        UUID id,
        String name,
        UUID parentId,
        int level,
        String path,
        boolean active,
        Instant createdAt
) {
    public static CategoryResponse fromEntity(CategoryEntity entity) {
        if (entity == null) {
            return null;
        }
        UUID parentId = entity.getParent() != null ? entity.getParent().getId() : null;
        return new CategoryResponse(
                entity.getId(),
                entity.getName(),
                parentId,
                entity.getLevel(),
                entity.getPath(),
                entity.isActive(),
                entity.getCreatedAt()
        );
    }
}
