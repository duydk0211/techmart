package com.techmart.core.dto.request.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @NotBlank(message = "Full name is required")
        @Size(min = 2, max = 100, message = "Full name must be 2-100 characters")
        String fullName,

        @Pattern(regexp = "^(0[3|5|7|8|9])+([0-9]{8})$", message = "Invalid Vietnamese phone format")
        String phone,

        @Size(max = 500, message = "Avatar URL max length is 500")
        String avatarUrl
) {}
