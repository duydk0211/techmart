package com.techmart.core.bus.command.catalog;

import com.techmart.core.bus.Command;
import com.techmart.core.dto.request.catalog.ProductCreateRequest;
import com.techmart.core.dto.response.catalog.ProductDetailResponse;

public record CreateProductCommand(
        ProductCreateRequest request
) implements Command<ProductDetailResponse> {
}
