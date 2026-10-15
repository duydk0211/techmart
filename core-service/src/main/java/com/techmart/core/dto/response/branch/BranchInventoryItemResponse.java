package com.techmart.core.dto.response.branch;

import java.util.Map;
import java.util.UUID;

public record BranchInventoryItemResponse(
        UUID variantId,
        String productName,
        String variantSku,
        Map<String, Object> variantAttributes,
        int quantityOnHand,
        int quantityReserved,
        int availableQuantity,
        int reorderLevel
) {
}
