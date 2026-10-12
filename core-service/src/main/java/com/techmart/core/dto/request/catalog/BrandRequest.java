package com.techmart.core.dto.request.catalog;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BrandRequest(
        @NotBlank(message = "Brand name is required")
        @Size(min = 1, max = 100, message = "Brand name must be between 1 and 100 characters")
        String name,

        String description,

        String logoUrl
) {
}
