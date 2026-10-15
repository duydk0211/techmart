package com.techmart.core.service.command.cart;

import com.techmart.core.dto.request.cart.AddToCartRequest;
import com.techmart.core.dto.request.cart.UpdateCartItemRequest;
import com.techmart.core.dto.response.cart.CartResponse;

import java.util.UUID;

public interface CartCommandService {

    CartResponse addToCart(UUID customerId, AddToCartRequest request);

    CartResponse updateCartItem(UUID customerId, UUID itemId, UpdateCartItemRequest request);

    void removeCartItem(UUID customerId, UUID itemId);

    void clearCart(UUID customerId);
}
