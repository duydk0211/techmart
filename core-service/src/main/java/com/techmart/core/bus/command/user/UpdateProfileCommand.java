package com.techmart.core.bus.command.user;

import com.techmart.core.bus.Command;
import com.techmart.core.dto.request.user.UpdateProfileRequest;
import com.techmart.core.dto.response.user.UserProfileResponse;

import java.util.UUID;

public record UpdateProfileCommand(UUID userId, UpdateProfileRequest request) implements Command<UserProfileResponse> {}
