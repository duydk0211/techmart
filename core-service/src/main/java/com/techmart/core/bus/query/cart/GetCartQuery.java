package com.techmart.core.bus.query.cart;

import com.techmart.core.bus.Query;
import com.techmart.core.dto.response.cart.CartResponse;

import java.util.UUID;

public record GetCartQuery(
        UUID customerId
) implements Query<CartResponse> {}
