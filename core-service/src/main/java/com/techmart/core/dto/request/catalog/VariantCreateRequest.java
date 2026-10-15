package com.techmart.core.dto.request.catalog;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record VariantCreateRequest(
        @NotBlank(message = "Variant SKU is required")
        @Size(max = 50, message = "SKU must not exceed 50 characters")
        String sku,

        Map<String, Object> attributes,

        @NotNull(message = "Variant price is required")
        @DecimalMin(value = "1.00", message = "Price must be at least 1.00")
        BigDecimal price,

        @DecimalMin(value = "0.00", message = "Discount percent must be >= 0")
        @DecimalMax(value = "100.00", message = "Discount percent must be <= 100")
        BigDecimal discountPercent,

        String imageUrl,

        @Valid
        List<InitialStockRequest> initialStockByBranch
) {
    public BigDecimal discountPercentOrDefault() {
        return discountPercent != null ? discountPercent : BigDecimal.ZERO;
    }
}
