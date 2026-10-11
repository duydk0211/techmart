package com.techmart.core.dto.response.user;

import com.techmart.core.entity.user.AddressItem;

import java.io.Serializable;
import java.util.UUID;

public record AddressResponse(
        UUID id,
        String name,
        String phone,
        String street,
        String province,
        String city,
        boolean isDefault
) implements Serializable {

    public static AddressResponse fromAddressItem(AddressItem item) {
        return new AddressResponse(
                item.id(),
                item.name(),
                item.phone(),
                item.street(),
                item.province(),
                item.city(),
                item.isDefault()
        );
    }
}
