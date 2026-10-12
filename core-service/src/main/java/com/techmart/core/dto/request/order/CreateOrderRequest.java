package com.techmart.core.dto.request.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record CreateOrderRequest(
        @NotNull(message = "Branch ID is required")
        UUID branchId,

        UUID addressId,

        @Valid
        NewAddressRequest newAddress,

        String note,

        @NotEmpty(message = "Items list cannot be empty")
        @Valid
        List<OrderItemRequest> items
) {}
