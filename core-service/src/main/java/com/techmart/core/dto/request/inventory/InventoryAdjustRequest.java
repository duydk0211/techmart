package com.techmart.core.dto.request.inventory;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

public record InventoryAdjustRequest(
        @NotNull(message = "Branch ID is required")
        UUID branchId,

        @NotNull(message = "Variant ID is required")
        UUID variantId,

        @NotNull(message = "Quantity change is required")
        int quantityChange,

        @NotBlank(message = "Change type is required")
        @Pattern(regexp = "STOCK_IN|STOCK_OUT|ADJUSTMENT", message = "Change type must be STOCK_IN, STOCK_OUT, or ADJUSTMENT")
        String changeType,

        String note
) {
}
