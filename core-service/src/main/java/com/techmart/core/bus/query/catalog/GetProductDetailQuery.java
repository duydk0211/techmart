package com.techmart.core.bus.query.catalog;

import com.techmart.core.bus.Query;
import com.techmart.core.dto.response.catalog.ProductDetailResponse;

import java.util.UUID;

public record GetProductDetailQuery(
        UUID id
) implements Query<ProductDetailResponse> {
}
