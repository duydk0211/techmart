package com.techmart.core.bus.query.user;

import com.techmart.core.bus.Query;
import com.techmart.core.dto.response.user.UserProfileResponse;

import java.util.UUID;

public record GetUserProfileQuery(UUID userId) implements Query<UserProfileResponse> {}
