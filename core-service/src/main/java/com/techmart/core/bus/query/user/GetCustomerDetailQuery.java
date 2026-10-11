package com.techmart.core.bus.query.user;

import com.techmart.core.bus.Query;
import com.techmart.core.dto.response.user.CustomerDetailResponse;

import java.util.UUID;

public record GetCustomerDetailQuery(UUID customerId) implements Query<CustomerDetailResponse> {}
