package com.techmart.core.dto.response.catalog;

import java.util.List;
import java.util.UUID;

public record ProductDetailResponse(
        UUID id,
        String name,
        String slug,
        String description,
        String status,
        CategoryDetailDto category,
        BrandSummaryDto brand,
        List<ProductImageResponse> images,
        List<VariantDetailResponse> variants
) {
    public record CategoryDetailDto(
            UUID id,
            String name,
            String breadcrumb
    ) {
    }
}
