package com.techmart.core.dto.response.cart;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

public record CartItemResponse(
        UUID id,
        UUID variantId,
        String productName,
        String variantSku,
        Map<String, Object> attributes,
        String imageUrl,
        BigDecimal currentPrice,
        BigDecimal discountPercent,
        BigDecimal effectiveUnitPrice,
        int quantity,
        BigDecimal itemSubtotal,
        boolean inStock
) {}
