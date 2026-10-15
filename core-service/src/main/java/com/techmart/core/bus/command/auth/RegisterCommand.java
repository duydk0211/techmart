package com.techmart.core.bus.command.auth;

import com.techmart.core.bus.Command;
import com.techmart.core.dto.request.auth.RegisterRequest;
import com.techmart.core.dto.response.user.UserProfileResponse;

public record RegisterCommand(RegisterRequest request) implements Command<UserProfileResponse> {}
