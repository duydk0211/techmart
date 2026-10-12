package com.techmart.core.dto.request.order;

import jakarta.validation.constraints.NotBlank;

public record UpdateOrderStatusRequest(
        @NotBlank(message = "Status is required")
        String status,

        String note
) {}
