package com.techmart.core.service.command.order;

import com.techmart.core.dto.request.order.CancelOrderRequest;
import com.techmart.core.dto.request.order.CreateOrderRequest;
import com.techmart.core.dto.request.order.UpdateOrderStatusRequest;
import com.techmart.core.dto.response.order.OrderResponse;

import java.util.UUID;

public interface OrderCommandService {

    OrderResponse createOrder(UUID customerId, CreateOrderRequest request);

    OrderResponse updateOrderStatus(UUID orderId, UpdateOrderStatusRequest request);

    OrderResponse cancelOrder(UUID orderId, CancelOrderRequest request, UUID customerId, String role);
}
