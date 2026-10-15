package com.techmart.core.dto.request.order;

import jakarta.validation.constraints.NotBlank;

public record NewAddressRequest(
        @NotBlank(message = "Name is required")
        String name,

        @NotBlank(message = "Phone is required")
        String phone,

        @NotBlank(message = "Street is required")
        String street,

        @NotBlank(message = "Province is required")
        String province,

        @NotBlank(message = "City is required")
        String city,

        boolean saveToAddressBook
) {}
