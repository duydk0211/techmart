package com.techmart.core.dto.response.order;

import com.techmart.core.entity.order.OrderEntity;
import com.techmart.core.entity.order.OrderItemEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderSummaryResponse(
        UUID id,
        String orderNumber,
        UUID branchId,
        String branchName,
        String status,
        BigDecimal totalAmount,
        int totalItems,
        Instant createdAt
) {
    public static OrderSummaryResponse fromEntity(OrderEntity entity) {
        if (entity == null) {
            return null;
        }

        int totalItems = entity.getItems() != null
                ? entity.getItems().stream().mapToInt(OrderItemEntity::getQuantity).sum()
                : 0;

        return new OrderSummaryResponse(
                entity.getId(),
                entity.getOrderNumber(),
                entity.getBranch() != null ? entity.getBranch().getId() : null,
                entity.getBranch() != null ? entity.getBranch().getName() : null,
                entity.getStatus() != null ? entity.getStatus().name() : null,
                entity.getTotalAmount(),
                totalItems,
                entity.getCreatedAt()
        );
    }
}
