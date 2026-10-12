package com.techmart.core.bus.query.order;

import com.techmart.core.bus.Query;
import com.techmart.core.dto.response.order.OrderResponse;

import java.util.UUID;

public record GetOrderDetailQuery(
        UUID orderId,
        UUID customerId,
        String role
) implements Query<OrderResponse> {}
