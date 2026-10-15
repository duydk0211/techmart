package com.techmart.core.bus.command.user;

import com.techmart.core.bus.Command;
import com.techmart.core.dto.response.user.AddressResponse;

import java.util.UUID;

public record SetDefaultAddressCommand(UUID userId, UUID addressId) implements Command<AddressResponse> {}
