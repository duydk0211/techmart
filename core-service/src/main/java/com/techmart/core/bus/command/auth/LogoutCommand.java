package com.techmart.core.bus.command.auth;

import com.techmart.core.bus.Command;
import com.techmart.core.dto.request.auth.LogoutRequest;

import java.util.UUID;

public record LogoutCommand(LogoutRequest request, UUID userId) implements Command<Void> {}
