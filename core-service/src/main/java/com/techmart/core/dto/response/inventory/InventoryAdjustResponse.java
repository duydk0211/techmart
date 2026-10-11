package com.techmart.core.dto.response.inventory;

import java.time.Instant;
import java.util.UUID;

public record InventoryAdjustResponse(
        UUID branchId,
        UUID variantId,
        int quantityOnHandBefore,
        int quantityOnHandAfter,
        int quantityReserved,
        int availableQuantity,
        Instant updatedAt
) {
}
