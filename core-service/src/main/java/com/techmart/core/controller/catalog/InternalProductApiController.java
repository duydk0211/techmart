package com.techmart.core.controller.catalog;

import com.techmart.core.dto.request.catalog.ProductCreateRequest;
import com.techmart.core.dto.request.catalog.VariantCreateRequest;
import com.techmart.core.dto.response.catalog.ProductDetailResponse;
import com.techmart.core.dto.response.catalog.ProductSummaryResponse;
import com.techmart.core.dto.response.catalog.VariantDetailResponse;
import com.techmart.shared.dto.PagedResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.UUID;

public interface InternalProductApiController {

    ResponseEntity<PagedResponse<ProductSummaryResponse>> searchProducts(
            String keyword,
            UUID categoryId,
            UUID brandId,
            UUID branchId,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Boolean inStockOnly,
            Pageable pageable
    );

    ResponseEntity<ProductDetailResponse> getProductDetail(UUID id);

    ResponseEntity<ProductDetailResponse> getProductBySlug(String slug);

    ResponseEntity<ProductDetailResponse> createProduct(ProductCreateRequest request);

    ResponseEntity<VariantDetailResponse> addVariant(UUID id, VariantCreateRequest request);
}
