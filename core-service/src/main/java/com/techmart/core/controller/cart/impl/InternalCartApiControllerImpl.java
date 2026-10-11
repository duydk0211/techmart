package com.techmart.core.controller.cart.impl;

import com.techmart.core.authorization.UserContext;
import com.techmart.core.bus.SpringBus;
import com.techmart.core.bus.command.cart.AddToCartCommand;
import com.techmart.core.bus.command.cart.ClearCartCommand;
import com.techmart.core.bus.command.cart.RemoveCartItemCommand;
import com.techmart.core.bus.command.cart.UpdateCartItemCommand;
import com.techmart.core.bus.query.cart.GetCartQuery;
import com.techmart.core.controller.cart.InternalCartApiController;
import com.techmart.core.dto.request.cart.AddToCartRequest;
import com.techmart.core.dto.request.cart.UpdateCartItemRequest;
import com.techmart.core.dto.response.cart.CartResponse;
import com.techmart.shared.constant.ResponseCode;
import com.techmart.shared.exception.CoreException;
import com.techmart.shared.security.TechmartPreauthorize;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
@RequestMapping("/internal/api/v1/cart")
public class InternalCartApiControllerImpl implements InternalCartApiController {

    private static final Logger log = LoggerFactory.getLogger(InternalCartApiControllerImpl.class);

    private final SpringBus springBus;

    public InternalCartApiControllerImpl(SpringBus springBus) {
        this.springBus = springBus;
    }

    private UUID getCurrentUserId() {
        UserContext.UserInfo user = UserContext.getCurrentUser();
        if (user == null || user.userId() == null) {
            throw new CoreException(ResponseCode.UNAUTHORIZED, "User authentication required");
        }
        return user.userId();
    }

    @Override
    @GetMapping
    @TechmartPreauthorize(gateway = "TECHMART_GW", roles = {"CUSTOMER"})
    public ResponseEntity<CartResponse> getCart() {
        UUID customerId = getCurrentUserId();
        log.info("[InternalCartApiControllerImpl][getCart] Fetching cart for customer: {}", customerId);
        CartResponse response = springBus.query(new GetCartQuery(customerId));
        return ResponseEntity.ok(response);
    }

    @Override
    @PostMapping("/items")
    @TechmartPreauthorize(gateway = "TECHMART_GW", roles = {"CUSTOMER"})
    public ResponseEntity<CartResponse> addToCart(@Valid @RequestBody AddToCartRequest request) {
        UUID customerId = getCurrentUserId();
        log.info("[InternalCartApiControllerImpl][addToCart] Adding item to cart for customer: {}", customerId);
        CartResponse response = springBus.execute(new AddToCartCommand(customerId, request));
        return ResponseEntity.ok(response);
    }

    @Override
    @PutMapping("/items/{id}")
    @TechmartPreauthorize(gateway = "TECHMART_GW", roles = {"CUSTOMER"})
    public ResponseEntity<CartResponse> updateCartItem(@PathVariable UUID id, @Valid @RequestBody UpdateCartItemRequest request) {
        UUID customerId = getCurrentUserId();
        log.info("[InternalCartApiControllerImpl][updateCartItem] Updating cart item {} for customer: {}", id, customerId);
        CartResponse response = springBus.execute(new UpdateCartItemCommand(customerId, id, request));
        return ResponseEntity.ok(response);
    }

    @Override
    @DeleteMapping("/items/{id}")
    @TechmartPreauthorize(gateway = "TECHMART_GW", roles = {"CUSTOMER"})
    public ResponseEntity<Void> removeCartItem(@PathVariable UUID id) {
        UUID customerId = getCurrentUserId();
        log.info("[InternalCartApiControllerImpl][removeCartItem] Removing cart item {} for customer: {}", id, customerId);
        springBus.execute(new RemoveCartItemCommand(customerId, id));
        return ResponseEntity.noContent().build();
    }

    @Override
    @DeleteMapping
    @TechmartPreauthorize(gateway = "TECHMART_GW", roles = {"CUSTOMER"})
    public ResponseEntity<Void> clearCart() {
        UUID customerId = getCurrentUserId();
        log.info("[InternalCartApiControllerImpl][clearCart] Clearing cart for customer: {}", customerId);
        springBus.execute(new ClearCartCommand(customerId));
        return ResponseEntity.noContent().build();
    }
}
