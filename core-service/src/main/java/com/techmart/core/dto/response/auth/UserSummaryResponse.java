package com.techmart.core.dto.response.auth;

import java.io.Serializable;
import java.util.UUID;

public record UserSummaryResponse(
        UUID id,
        String email,
        String fullName,
        String role
) implements Serializable {}
