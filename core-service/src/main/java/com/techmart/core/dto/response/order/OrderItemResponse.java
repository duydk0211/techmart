package com.techmart.core.dto.response.order;

import com.techmart.core.entity.order.OrderItemEntity;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

public record OrderItemResponse(
        UUID id,
        UUID variantId,
        String productName,
        String variantSku,
        Map<String, Object> variantAttributes,
        BigDecimal unitPrice,
        BigDecimal discountPercent,
        int quantity,
        BigDecimal subtotal
) {
    public static OrderItemResponse fromEntity(OrderItemEntity entity) {
        if (entity == null) {
            return null;
        }
        return new OrderItemResponse(
                entity.getId(),
                entity.getVariant() != null ? entity.getVariant().getId() : null,
                entity.getProductName(),
                entity.getVariantSku(),
                entity.getVariantAttributes(),
                entity.getUnitPrice(),
                entity.getDiscountPercent(),
                entity.getQuantity(),
                entity.getSubtotal()
        );
    }
}
