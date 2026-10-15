package com.techmart.core.dto.request.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AddressRequest(
        @NotBlank(message = "Recipient name is required")
        @Size(min = 2, max = 100, message = "Recipient name must be 2-100 characters")
        String name,

        @NotBlank(message = "Phone number is required")
        @Pattern(regexp = "^(0[3|5|7|8|9])+([0-9]{8})$", message = "Invalid Vietnamese phone format")
        String phone,

        @NotBlank(message = "Street address is required")
        @Size(max = 255, message = "Street address max length is 255")
        String street,

        @NotBlank(message = "Province is required")
        @Size(max = 100, message = "Province max length is 100")
        String province,

        @NotBlank(message = "City is required")
        @Size(max = 100, message = "City max length is 100")
        String city,

        boolean isDefault
) {}
