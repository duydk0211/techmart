package com.techmart.core.bus.command.auth;

import com.techmart.core.bus.Command;
import com.techmart.core.dto.request.auth.ChangePasswordRequest;

import java.util.UUID;

public record ChangePasswordCommand(UUID userId, ChangePasswordRequest request) implements Command<Void> {}
