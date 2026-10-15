package com.techmart.core.dto.response.metrics;

import java.util.Map;
import java.util.UUID;

/**
 * Stock alert detail for inventory variants nearing exhaustion or depleted.
 */
public record StockAlertItemResponse(
        UUID branchId,
        String branchName,
        UUID productId,
        String productName,
        UUID variantId,
        String sku,
        Map<String, Object> attributes,
        int quantityOnHand,
        int quantityReserved,
        int quantityAvailable,
        int reorderLevel,
        String alertStatus
) {}
