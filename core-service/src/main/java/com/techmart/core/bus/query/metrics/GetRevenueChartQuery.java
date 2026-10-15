package com.techmart.core.bus.query.metrics;

import com.techmart.core.bus.Query;
import com.techmart.core.dto.response.metrics.RevenueChartResponse;

import java.util.UUID;

public record GetRevenueChartQuery(
        String period,
        UUID branchId
) implements Query<RevenueChartResponse> {}
