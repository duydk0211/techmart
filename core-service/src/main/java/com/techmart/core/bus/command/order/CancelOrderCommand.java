package com.techmart.core.bus.command.order;

import com.techmart.core.bus.Command;
import com.techmart.core.dto.request.order.CancelOrderRequest;
import com.techmart.core.dto.response.order.OrderResponse;

import java.util.UUID;

public record CancelOrderCommand(
        UUID orderId,
        CancelOrderRequest request,
        UUID customerId,
        String role
) implements Command<OrderResponse> {}
