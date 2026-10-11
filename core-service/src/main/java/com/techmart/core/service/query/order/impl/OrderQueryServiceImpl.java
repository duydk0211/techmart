package com.techmart.core.service.query.order.impl;

import com.techmart.core.dto.response.order.OrderResponse;
import com.techmart.core.dto.response.order.OrderSummaryResponse;
import com.techmart.core.entity.order.OrderEntity;
import com.techmart.core.entity.order.OrderStatus;
import com.techmart.core.repository.order.OrderRepository;
import com.techmart.core.service.query.order.OrderQueryService;
import com.techmart.shared.constant.ResponseCode;
import com.techmart.shared.dto.PagedResponse;
import com.techmart.shared.exception.CoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class OrderQueryServiceImpl implements OrderQueryService {

    private static final Logger log = LoggerFactory.getLogger(OrderQueryServiceImpl.class);

    private final OrderRepository orderRepository;

    public OrderQueryServiceImpl(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    public PagedResponse<OrderSummaryResponse> getMyOrders(UUID customerId, Pageable pageable) {
        log.info("[OrderQueryServiceImpl][getMyOrders] Fetching orders for customer: {}", customerId);

        Page<OrderEntity> page = orderRepository.findByCustomerIdOrderByCreatedAtDesc(customerId, pageable);
        List<OrderSummaryResponse> content = page.getContent().stream()
                .map(OrderSummaryResponse::fromEntity)
                .toList();

        return PagedResponse.of(content, page.getNumber(), page.getSize(), page.getTotalElements());
    }

    @Override
    public OrderResponse getOrderDetail(UUID orderId, UUID customerId, String role) {
        log.info("[OrderQueryServiceImpl][getOrderDetail] Fetching order {} by user {} (role: {})", orderId, customerId, role);

        OrderEntity order = orderRepository.findByIdWithDetails(orderId)
                .orElseThrow(() -> new CoreException(ResponseCode.RESOURCE_NOT_FOUND, "Order not found: " + orderId));

        boolean isStaffOrAdmin = "STAFF".equalsIgnoreCase(role) || "ADMIN".equalsIgnoreCase(role);
        if (!isStaffOrAdmin && !order.getCustomerId().equals(customerId)) {
            log.warn("[OrderQueryServiceImpl][getOrderDetail] Forbidden: user {} tried to access order {} owned by {}",
                    customerId, orderId, order.getCustomerId());
            throw new CoreException(ResponseCode.FORBIDDEN_RESOURCE, "You do not have permission to view this order");
        }

        return OrderResponse.fromEntity(order);
    }

    @Override
    public PagedResponse<OrderSummaryResponse> getOrders(String status, Pageable pageable) {
        log.info("[OrderQueryServiceImpl][getOrders] Fetching all orders with status filter: {}", status);

        Page<OrderEntity> page;
        if (status != null && !status.isBlank()) {
            try {
                OrderStatus orderStatus = OrderStatus.valueOf(status.trim().toUpperCase());
                page = orderRepository.findByStatusOrderByCreatedAtDesc(orderStatus, pageable);
            } catch (IllegalArgumentException e) {
                throw new CoreException(ResponseCode.VALIDATION_FAILED, "Invalid status parameter: " + status);
            }
        } else {
            page = orderRepository.findAllByOrderByCreatedAtDesc(pageable);
        }

        List<OrderSummaryResponse> content = page.getContent().stream()
                .map(OrderSummaryResponse::fromEntity)
                .toList();

        return PagedResponse.of(content, page.getNumber(), page.getSize(), page.getTotalElements());
    }
}
