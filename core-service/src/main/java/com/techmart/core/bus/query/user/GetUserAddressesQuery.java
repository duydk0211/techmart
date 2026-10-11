package com.techmart.core.bus.query.user;

import com.techmart.core.bus.Query;
import com.techmart.core.dto.response.user.AddressResponse;

import java.util.List;
import java.util.UUID;

public record GetUserAddressesQuery(UUID userId) implements Query<List<AddressResponse>> {}
