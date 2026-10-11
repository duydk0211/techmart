package com.techmart.core.bus.query.catalog;

import com.techmart.core.bus.Query;
import com.techmart.core.dto.response.catalog.ProductDetailResponse;

public record GetProductBySlugQuery(
        String slug
) implements Query<ProductDetailResponse> {
}
