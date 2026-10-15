package com.techmart.core.bus.command.auth;

import com.techmart.core.bus.Command;
import com.techmart.core.dto.request.auth.LoginRequest;
import com.techmart.core.dto.response.auth.AuthResponse;

public record LoginCommand(LoginRequest request) implements Command<AuthResponse> {}
