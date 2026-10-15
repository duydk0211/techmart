package com.techmart.core.bus.command.order;

import com.techmart.core.bus.Command;
import com.techmart.core.dto.request.order.UpdateOrderStatusRequest;
import com.techmart.core.dto.response.order.OrderResponse;

import java.util.UUID;

public record UpdateOrderStatusCommand(
        UUID orderId,
        UpdateOrderStatusRequest request
) implements Command<OrderResponse> {}
