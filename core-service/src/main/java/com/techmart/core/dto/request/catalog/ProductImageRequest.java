package com.techmart.core.dto.request.catalog;

import jakarta.validation.constraints.NotBlank;

public record ProductImageRequest(
        @NotBlank(message = "Image URL is required")
        String imageUrl,

        Boolean isPrimary,

        Integer displayOrder
) {
    public boolean primaryOrDefault() {
        return isPrimary != null && isPrimary;
    }

    public int displayOrderOrDefault() {
        return displayOrder != null ? displayOrder : 0;
    }
}
