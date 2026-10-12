package com.techmart.core.dto.response.catalog;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record VariantDetailResponse(
        UUID id,
        String sku,
        Map<String, Object> attributes,
        BigDecimal price,
        BigDecimal discountPercent,
        BigDecimal effectivePrice,
        String imageUrl,
        String status,
        int totalAvailableStock,
        List<StockDistributionDto> stockDistribution
) {
}
