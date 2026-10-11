package com.techmart.core.dto.request.catalog;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record ProductCreateRequest(
        @NotBlank(message = "Product name is required")
        @Size(max = 255, message = "Product name must not exceed 255 characters")
        String name,

        String slug,

        String description,

        String thumbnailUrl,

        @NotNull(message = "Category ID is required")
        UUID categoryId,

        @NotNull(message = "Brand ID is required")
        UUID brandId,

        @Valid
        List<ProductImageRequest> images
) {
}
