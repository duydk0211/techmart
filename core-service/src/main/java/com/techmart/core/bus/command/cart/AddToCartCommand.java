package com.techmart.core.bus.command.cart;

import com.techmart.core.bus.Command;
import com.techmart.core.dto.request.cart.AddToCartRequest;
import com.techmart.core.dto.response.cart.CartResponse;

import java.util.UUID;

public record AddToCartCommand(
        UUID customerId,
        AddToCartRequest request
) implements Command<CartResponse> {}
