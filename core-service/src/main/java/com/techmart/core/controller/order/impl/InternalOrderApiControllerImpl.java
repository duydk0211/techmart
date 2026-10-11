package com.techmart.core.controller.order.impl;

import com.techmart.core.authorization.UserContext;
import com.techmart.core.bus.SpringBus;
import com.techmart.core.bus.command.order.CancelOrderCommand;
import com.techmart.core.bus.command.order.CreateOrderCommand;
import com.techmart.core.bus.command.order.UpdateOrderStatusCommand;
import com.techmart.core.bus.query.order.GetMyOrdersQuery;
import com.techmart.core.bus.query.order.GetOrderDetailQuery;
import com.techmart.core.bus.query.order.GetOrdersQuery;
import com.techmart.core.controller.order.InternalOrderApiController;
import com.techmart.core.dto.request.order.CancelOrderRequest;
import com.techmart.core.dto.request.order.CreateOrderRequest;
import com.techmart.core.dto.request.order.UpdateOrderStatusRequest;
import com.techmart.core.dto.response.order.OrderResponse;
import com.techmart.core.dto.response.order.OrderSummaryResponse;
import com.techmart.shared.constant.ResponseCode;
import com.techmart.shared.dto.PagedResponse;
import com.techmart.shared.exception.CoreException;
import com.techmart.shared.security.TechmartPreauthorize;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/internal/api/v1/orders")
public class InternalOrderApiControllerImpl implements InternalOrderApiController {

    private static final Logger log = LoggerFactory.getLogger(InternalOrderApiControllerImpl.class);

    private final SpringBus springBus;

    public InternalOrderApiControllerImpl(SpringBus springBus) {
        this.springBus = springBus;
    }

    private UserContext.UserInfo getCurrentUser() {
        UserContext.UserInfo user = UserContext.getCurrentUser();
        if (user == null || user.userId() == null) {
            throw new CoreException(ResponseCode.UNAUTHORIZED, "User authentication required");
        }
        return user;
    }

    @Override
    @PostMapping
    @TechmartPreauthorize(gateway = "TECHMART_GW", roles = {"CUSTOMER", "STAFF"})
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        UserContext.UserInfo user = getCurrentUser();
        log.info("[InternalOrderApiControllerImpl][createOrder] Creating order for user: {}", user.userId());
        OrderResponse response = springBus.execute(new CreateOrderCommand(user.userId(), request));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Override
    @GetMapping("/me")
    @TechmartPreauthorize(gateway = "TECHMART_GW", roles = {"CUSTOMER"})
    public ResponseEntity<PagedResponse<OrderSummaryResponse>> getMyOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UserContext.UserInfo user = getCurrentUser();
        log.info("[InternalOrderApiControllerImpl][getMyOrders] Fetching orders for user: {}", user.userId());
        Pageable pageable = PageRequest.of(page, size);
        PagedResponse<OrderSummaryResponse> response = springBus.query(new GetMyOrdersQuery(user.userId(), pageable));
        return ResponseEntity.ok(response);
    }

    @Override
    @GetMapping("/{id}")
    @TechmartPreauthorize(gateway = "TECHMART_GW", roles = {"CUSTOMER", "STAFF", "ADMIN"})
    public ResponseEntity<OrderResponse> getOrderDetail(@PathVariable UUID id) {
        UserContext.UserInfo user = getCurrentUser();
        log.info("[InternalOrderApiControllerImpl][getOrderDetail] Fetching order {} by user: {}", id, user.userId());
        OrderResponse response = springBus.query(new GetOrderDetailQuery(id, user.userId(), user.role()));
        return ResponseEntity.ok(response);
    }

    @Override
    @GetMapping
    @TechmartPreauthorize(gateway = "TECHMART_GW", roles = {"STAFF", "ADMIN"})
    public ResponseEntity<PagedResponse<OrderSummaryResponse>> getOrders(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("[InternalOrderApiControllerImpl][getOrders] Fetching all orders with status: {}, page: {}, size: {}", status, page, size);
        Pageable pageable = PageRequest.of(page, size);
        PagedResponse<OrderSummaryResponse> response = springBus.query(new GetOrdersQuery(status, pageable));
        return ResponseEntity.ok(response);
    }

    @Override
    @PutMapping("/{id}/status")
    @TechmartPreauthorize(gateway = "TECHMART_GW", roles = {"STAFF", "ADMIN"})
    public ResponseEntity<OrderResponse> updateOrderStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateOrderStatusRequest request) {
        log.info("[InternalOrderApiControllerImpl][updateOrderStatus] Updating status for order: {} to {}", id, request.status());
        OrderResponse response = springBus.execute(new UpdateOrderStatusCommand(id, request));
        return ResponseEntity.ok(response);
    }

    @Override
    @PostMapping("/{id}/cancel")
    @TechmartPreauthorize(gateway = "TECHMART_GW", roles = {"CUSTOMER", "STAFF", "ADMIN"})
    public ResponseEntity<OrderResponse> cancelOrder(
            @PathVariable UUID id,
            @RequestBody(required = false) CancelOrderRequest request) {
        UserContext.UserInfo user = getCurrentUser();
        log.info("[InternalOrderApiControllerImpl][cancelOrder] Cancelling order: {} requested by user: {}", id, user.userId());
        OrderResponse response = springBus.execute(new CancelOrderCommand(id, request, user.userId(), user.role()));
        return ResponseEntity.ok(response);
    }
}
