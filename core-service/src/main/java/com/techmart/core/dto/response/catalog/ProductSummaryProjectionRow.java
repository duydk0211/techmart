package com.techmart.core.dto.response.catalog;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Flat constructor projection row mapped directly from JPQL aggregation query.
 * Eliminates N+1 queries by fetching product metadata, price boundaries, variant count,
 * and stock availability in a single SQL operation.
 */
public record ProductSummaryProjectionRow(
        UUID id,
        String name,
        String slug,
        UUID categoryId,
        String categoryName,
        UUID brandId,
        String brandName,
        String thumbnailUrl,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        Long variantCount,
        Long inStockCount,
        String status
) {
}
