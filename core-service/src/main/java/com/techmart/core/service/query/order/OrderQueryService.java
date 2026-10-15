package com.techmart.core.service.query.order;

import com.techmart.core.dto.response.order.OrderResponse;
import com.techmart.core.dto.response.order.OrderSummaryResponse;
import com.techmart.shared.dto.PagedResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface OrderQueryService {

    PagedResponse<OrderSummaryResponse> getMyOrders(UUID customerId, Pageable pageable);

    OrderResponse getOrderDetail(UUID orderId, UUID customerId, String role);

    PagedResponse<OrderSummaryResponse> getOrders(String status, Pageable pageable);
}
