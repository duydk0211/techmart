package com.techmart.core.service.query.cart;

import com.techmart.core.dto.response.cart.CartResponse;

import java.util.UUID;

public interface CartQueryService {

    CartResponse getCart(UUID customerId);
}
