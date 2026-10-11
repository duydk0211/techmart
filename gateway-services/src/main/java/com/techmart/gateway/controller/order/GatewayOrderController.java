package com.techmart.gateway.controller.order;

import com.techmart.gateway.client.CoreClient;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
public class GatewayOrderController {

    private static final Logger log = LoggerFactory.getLogger(GatewayOrderController.class);

    private final CoreClient coreClient;

    public GatewayOrderController(CoreClient coreClient) {
        this.coreClient = coreClient;
    }

    @PostMapping
    public ResponseEntity<Object> createOrder(@RequestBody Object body) {
        log.info("[GatewayOrderController][createOrder] Proxying createOrder to core-service");
        return coreClient.forward(HttpMethod.POST, "/internal/api/v1/orders", body, Object.class);
    }

    @GetMapping("/me")
    public ResponseEntity<Object> getMyOrders(HttpServletRequest request) {
        log.info("[GatewayOrderController][getMyOrders] Proxying getMyOrders to core-service");
        String query = request.getQueryString();
        String uri = "/internal/api/v1/orders/me" + (query != null ? "?" + query : "");
        return coreClient.forward(HttpMethod.GET, uri, null, Object.class);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Object> getOrderDetail(@PathVariable UUID id) {
        log.info("[GatewayOrderController][getOrderDetail] Proxying getOrderDetail to core-service for order: {}", id);
        return coreClient.forward(HttpMethod.GET, "/internal/api/v1/orders/" + id, null, Object.class);
    }

    @GetMapping
    public ResponseEntity<Object> getOrders(HttpServletRequest request) {
        log.info("[GatewayOrderController][getOrders] Proxying getOrders to core-service");
        String query = request.getQueryString();
        String uri = "/internal/api/v1/orders" + (query != null ? "?" + query : "");
        return coreClient.forward(HttpMethod.GET, uri, null, Object.class);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Object> updateOrderStatus(@PathVariable UUID id, @RequestBody Object body) {
        log.info("[GatewayOrderController][updateOrderStatus] Proxying updateOrderStatus to core-service for order: {}", id);
        return coreClient.forward(HttpMethod.PUT, "/internal/api/v1/orders/" + id + "/status", body, Object.class);
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<Object> cancelOrder(@PathVariable UUID id, @RequestBody(required = false) Object body) {
        log.info("[GatewayOrderController][cancelOrder] Proxying cancelOrder to core-service for order: {}", id);
        return coreClient.forward(HttpMethod.POST, "/internal/api/v1/orders/" + id + "/cancel", body, Object.class);
    }
}
