package com.techmart.core.dto.request.order;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record OrderItemRequest(
        @NotNull(message = "Variant ID is required")
        UUID variantId,

        @Min(value = 1, message = "Quantity must be at least 1")
        int quantity
) {}
