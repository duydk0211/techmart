package com.techmart.core.dto.response.catalog;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductSummaryResponse(
        UUID id,
        String name,
        String slug,
        CategorySummaryDto category,
        BrandSummaryDto brand,
        String thumbnailUrl,
        PriceRangeDto price,
        int variantCount,
        String availability,
        String status
) {
    public static ProductSummaryResponse fromProjectionRow(ProductSummaryProjectionRow row) {
        if (row == null) {
            return null;
        }

        BigDecimal min = row.minPrice() != null ? row.minPrice() : BigDecimal.ZERO;
        BigDecimal max = row.maxPrice() != null ? row.maxPrice() : BigDecimal.ZERO;
        PriceRangeDto priceRange = new PriceRangeDto(min, max);

        CategorySummaryDto category = new CategorySummaryDto(row.categoryId(), row.categoryName());
        BrandSummaryDto brand = new BrandSummaryDto(row.brandId(), row.brandName());

        int count = row.variantCount() != null ? row.variantCount().intValue() : 0;
        String availability = (row.inStockCount() != null && row.inStockCount() > 0) ? "IN_STOCK" : "OUT_OF_STOCK";

        return new ProductSummaryResponse(
                row.id(),
                row.name(),
                row.slug(),
                category,
                brand,
                row.thumbnailUrl(),
                priceRange,
                count,
                availability,
                row.status()
        );
    }
}
