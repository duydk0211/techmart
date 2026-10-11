package com.techmart.core.bus.query.order;

import com.techmart.core.bus.Query;
import com.techmart.core.dto.response.order.OrderSummaryResponse;
import com.techmart.shared.dto.PagedResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public record GetMyOrdersQuery(
        UUID customerId,
        Pageable pageable
) implements Query<PagedResponse<OrderSummaryResponse>> {}
