package com.techmart.core.dto.response.catalog;

import com.techmart.core.entity.catalog.CategoryEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record CategoryTreeResponse(
        UUID id,
        String name,
        int level,
        String path,
        boolean active,
        List<CategoryTreeResponse> children
) {
    public static CategoryTreeResponse fromEntity(CategoryEntity entity) {
        if (entity == null) {
            return null;
        }
        return new CategoryTreeResponse(
                entity.getId(),
                entity.getName(),
                entity.getLevel(),
                entity.getPath(),
                entity.isActive(),
                new ArrayList<>()
        );
    }
}
