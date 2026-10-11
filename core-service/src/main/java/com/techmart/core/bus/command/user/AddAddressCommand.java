package com.techmart.core.bus.command.user;

import com.techmart.core.bus.Command;
import com.techmart.core.dto.request.user.AddressRequest;
import com.techmart.core.dto.response.user.AddressResponse;

import java.util.UUID;

public record AddAddressCommand(UUID userId, AddressRequest request) implements Command<AddressResponse> {}
