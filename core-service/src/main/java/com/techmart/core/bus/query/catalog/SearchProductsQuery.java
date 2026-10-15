package com.techmart.core.bus.query.catalog;

import com.techmart.core.bus.Query;
import com.techmart.core.dto.response.catalog.ProductSummaryResponse;
import com.techmart.shared.dto.PagedResponse;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.UUID;

public record SearchProductsQuery(
        String keyword,
        UUID categoryId,
        UUID brandId,
        UUID branchId,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        Boolean inStockOnly,
        Pageable pageable
) implements Query<PagedResponse<ProductSummaryResponse>> {
}
