package com.techmart.core.dto.request.branch;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BranchRequest(
        @NotBlank(message = "Branch name is required")
        @Size(min = 1, max = 100, message = "Branch name must be between 1 and 100 characters")
        String name,

        @NotBlank(message = "Branch address is required")
        String address,

        String phone
) {
}
