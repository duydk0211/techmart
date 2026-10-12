package com.techmart.core.entity.user;

import java.io.Serializable;
import java.util.UUID;

public record AddressItem(
        UUID id,
        String name,
        String phone,
        String street,
        String province,
        String city,
        boolean isDefault
) implements Serializable {}
