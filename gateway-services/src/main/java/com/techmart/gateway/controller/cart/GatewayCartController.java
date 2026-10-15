package com.techmart.gateway.controller.cart;

import com.techmart.gateway.client.CoreClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cart")
public class GatewayCartController {

    private static final Logger log = LoggerFactory.getLogger(GatewayCartController.class);

    private final CoreClient coreClient;

    public GatewayCartController(CoreClient coreClient) {
        this.coreClient = coreClient;
    }

    @GetMapping
    public ResponseEntity<Object> getCart() {
        log.info("[GatewayCartController][getCart] Proxying getCart to core-service");
        return coreClient.forward(HttpMethod.GET, "/internal/api/v1/cart", null, Object.class);
    }

    @PostMapping("/items")
    public ResponseEntity<Object> addToCart(@RequestBody Object body) {
        log.info("[GatewayCartController][addToCart] Proxying addToCart to core-service");
        return coreClient.forward(HttpMethod.POST, "/internal/api/v1/cart/items", body, Object.class);
    }

    @PutMapping("/items/{id}")
    public ResponseEntity<Object> updateCartItem(@PathVariable UUID id, @RequestBody Object body) {
        log.info("[GatewayCartController][updateCartItem] Proxying updateCartItem to core-service for item: {}", id);
        return coreClient.forward(HttpMethod.PUT, "/internal/api/v1/cart/items/" + id, body, Object.class);
    }

    @DeleteMapping("/items/{id}")
    public ResponseEntity<Void> removeCartItem(@PathVariable UUID id) {
        log.info("[GatewayCartController][removeCartItem] Proxying removeCartItem to core-service for item: {}", id);
        return coreClient.forward(HttpMethod.DELETE, "/internal/api/v1/cart/items/" + id, null, Void.class);
    }

    @DeleteMapping
    public ResponseEntity<Void> clearCart() {
        log.info("[GatewayCartController][clearCart] Proxying clearCart to core-service");
        return coreClient.forward(HttpMethod.DELETE, "/internal/api/v1/cart", null, Void.class);
    }
}
