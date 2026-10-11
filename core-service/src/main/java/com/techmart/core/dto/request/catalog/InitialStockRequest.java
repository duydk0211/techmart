package com.techmart.core.dto.request.catalog;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record InitialStockRequest(
        @NotNull(message = "Branch ID is required")
        UUID branchId,

        @Min(value = 0, message = "Initial quantity on hand must be >= 0")
        int quantityOnHand,

        Integer reorderLevel
) {
    public int reorderLevelOrDefault() {
        return reorderLevel != null ? reorderLevel : 5;
    }
}
