package com.techmart.core.bus.command.order;

import com.techmart.core.bus.Command;
import com.techmart.core.dto.request.order.CreateOrderRequest;
import com.techmart.core.dto.response.order.OrderResponse;

import java.util.UUID;

public record CreateOrderCommand(
        UUID customerId,
        CreateOrderRequest request
) implements Command<OrderResponse> {}
