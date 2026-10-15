package com.techmart.core.bus.command.user;

import com.techmart.core.bus.Command;
import com.techmart.core.dto.request.user.CreateStaffRequest;
import com.techmart.core.dto.response.user.UserProfileResponse;

import java.util.UUID;

public record CreateStaffCommand(CreateStaffRequest request, UUID createdBy) implements Command<UserProfileResponse> {}
