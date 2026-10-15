package com.techmart.core.dto.response.order;

import com.techmart.core.entity.order.OrderEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        String orderNumber,
        BranchSummaryDto branch,
        String status,
        BigDecimal totalAmount,
        String shippingAddress,
        String customerName,
        String customerPhone,
        String customerEmail,
        String note,
        String cancelledReason,
        Instant createdAt,
        List<OrderItemResponse> items
) {
    public record BranchSummaryDto(UUID id, String name) {}

    public static OrderResponse fromEntity(OrderEntity entity) {
        if (entity == null) {
            return null;
        }

        BranchSummaryDto branchDto = entity.getBranch() != null
                ? new BranchSummaryDto(entity.getBranch().getId(), entity.getBranch().getName())
                : null;

        List<OrderItemResponse> itemDtos = entity.getItems() != null
                ? entity.getItems().stream().map(OrderItemResponse::fromEntity).toList()
                : List.of();

        return new OrderResponse(
                entity.getId(),
                entity.getOrderNumber(),
                branchDto,
                entity.getStatus() != null ? entity.getStatus().name() : null,
                entity.getTotalAmount(),
                entity.getShippingAddress(),
                entity.getCustomerName(),
                entity.getCustomerPhone(),
                entity.getCustomerEmail(),
                entity.getNote(),
                entity.getCancelledReason(),
                entity.getCreatedAt(),
                itemDtos
        );
    }
}
