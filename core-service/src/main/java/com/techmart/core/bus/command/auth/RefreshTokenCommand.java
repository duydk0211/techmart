package com.techmart.core.bus.command.auth;

import com.techmart.core.bus.Command;
import com.techmart.core.dto.request.auth.RefreshTokenRequest;
import com.techmart.core.dto.response.auth.AuthResponse;

public record RefreshTokenCommand(RefreshTokenRequest request) implements Command<AuthResponse> {}
