package com.techmart.core.bus.command.cart;

import com.techmart.core.bus.Command;

import java.util.UUID;

public record ClearCartCommand(
        UUID customerId
) implements Command<Void> {}
