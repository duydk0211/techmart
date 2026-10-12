package com.techmart.core.bus.query.order;

import com.techmart.core.bus.Query;
import com.techmart.core.dto.response.order.OrderSummaryResponse;
import com.techmart.shared.dto.PagedResponse;
import org.springframework.data.domain.Pageable;

public record GetOrdersQuery(
        String status,
        Pageable pageable
) implements Query<PagedResponse<OrderSummaryResponse>> {}
