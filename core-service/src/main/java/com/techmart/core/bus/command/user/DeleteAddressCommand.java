package com.techmart.core.bus.command.user;

import com.techmart.core.bus.Command;

import java.util.UUID;

public record DeleteAddressCommand(UUID userId, UUID addressId) implements Command<Void> {}
