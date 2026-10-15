package com.techmart.core.dto.request.catalog;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BannerRequest(
        @NotBlank(message = "Banner title is required")
        @Size(max = 200, message = "Banner title must not exceed 200 characters")
        String title,

        @NotBlank(message = "Image URL is required")
        @Size(max = 500, message = "Image URL must not exceed 500 characters")
        String imageUrl,

        @Size(max = 500, message = "Target URL must not exceed 500 characters")
        String targetUrl,

        Integer displayOrder,

        Boolean active
) {
}
