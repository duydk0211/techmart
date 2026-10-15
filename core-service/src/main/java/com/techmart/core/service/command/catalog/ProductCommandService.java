package com.techmart.core.service.command.catalog;

import com.techmart.core.dto.request.catalog.ProductCreateRequest;
import com.techmart.core.dto.request.catalog.VariantCreateRequest;
import com.techmart.core.dto.response.catalog.ProductDetailResponse;
import com.techmart.core.dto.response.catalog.VariantDetailResponse;

import java.util.UUID;

public interface ProductCommandService {

    ProductDetailResponse createProduct(ProductCreateRequest request);

    VariantDetailResponse addVariant(UUID productId, VariantCreateRequest request);
}
