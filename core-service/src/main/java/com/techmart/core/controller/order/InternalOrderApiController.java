package com.techmart.core.controller.order;

import com.techmart.core.dto.request.order.CancelOrderRequest;
import com.techmart.core.dto.request.order.CreateOrderRequest;
import com.techmart.core.dto.request.order.UpdateOrderStatusRequest;
import com.techmart.core.dto.response.order.OrderResponse;
import com.techmart.core.dto.response.order.OrderSummaryResponse;
import com.techmart.shared.dto.PagedResponse;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

public interface InternalOrderApiController {

    ResponseEntity<OrderResponse> createOrder(CreateOrderRequest request);

    ResponseEntity<PagedResponse<OrderSummaryResponse>> getMyOrders(int page, int size);

    ResponseEntity<OrderResponse> getOrderDetail(UUID id);

    ResponseEntity<PagedResponse<OrderSummaryResponse>> getOrders(String status, int page, int size);

    ResponseEntity<OrderResponse> updateOrderStatus(UUID id, UpdateOrderStatusRequest request);

    ResponseEntity<OrderResponse> cancelOrder(UUID id, CancelOrderRequest request);
}
