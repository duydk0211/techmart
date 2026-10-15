package com.techmart.core.controller.cart;

import com.techmart.core.dto.request.cart.AddToCartRequest;
import com.techmart.core.dto.request.cart.UpdateCartItemRequest;
import com.techmart.core.dto.response.cart.CartResponse;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

public interface InternalCartApiController {

    ResponseEntity<CartResponse> getCart();

    ResponseEntity<CartResponse> addToCart(AddToCartRequest request);

    ResponseEntity<CartResponse> updateCartItem(UUID id, UpdateCartItemRequest request);

    ResponseEntity<Void> removeCartItem(UUID id);

    ResponseEntity<Void> clearCart();
}
