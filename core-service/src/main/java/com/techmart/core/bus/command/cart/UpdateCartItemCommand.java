package com.techmart.core.bus.command.cart;

import com.techmart.core.bus.Command;
import com.techmart.core.dto.request.cart.UpdateCartItemRequest;
import com.techmart.core.dto.response.cart.CartResponse;

import java.util.UUID;

public record UpdateCartItemCommand(
        UUID customerId,
        UUID itemId,
        UpdateCartItemRequest request
) implements Command<CartResponse> {}
