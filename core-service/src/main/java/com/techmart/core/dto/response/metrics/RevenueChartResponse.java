package com.techmart.core.dto.response.metrics;

import java.math.BigDecimal;
import java.util.List;

/**
 * Historical revenue and order volume timeline chart response.
 */
public record RevenueChartResponse(
        String period,
        List<TimelinePoint> timeline,
        BigDecimal totalPeriodRevenue,
        long totalPeriodOrders
) {
    public record TimelinePoint(
            String timestamp,
            BigDecimal revenue,
            long ordersCount
    ) {}
}
