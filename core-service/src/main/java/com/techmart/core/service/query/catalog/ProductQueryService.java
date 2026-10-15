package com.techmart.core.service.query.catalog;

import com.techmart.core.bus.query.catalog.SearchProductsQuery;
import com.techmart.core.dto.response.catalog.ProductDetailResponse;
import com.techmart.core.dto.response.catalog.ProductSummaryResponse;
import com.techmart.shared.dto.PagedResponse;

import java.util.UUID;

public interface ProductQueryService {

    PagedResponse<ProductSummaryResponse> searchProducts(SearchProductsQuery query);

    ProductDetailResponse getProductDetail(UUID id);

    ProductDetailResponse getProductBySlug(String slug);
}
