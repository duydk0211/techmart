package com.techmart.core.dto.response.metrics;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * High-level executive dashboard summary KPI metrics.
 */
public record DashboardSummaryResponse(
        RevenueKpi revenue,
        OrdersKpi orders,
        InventoryKpi inventory,
        CustomersKpi customers,
        Instant calculatedAt
) {
    public record RevenueKpi(
            BigDecimal todayRevenue,
            BigDecimal yesterdayRevenue,
            Double growthRatePercent
    ) {}

    public record OrdersKpi(
            long todayOrdersTotal,
            long pendingCount,
            long confirmedCount,
            long processingCount,
            long shippedCount,
            long deliveredCount,
            long cancelledCount
    ) {}

    public record InventoryKpi(
            long lowStockVariantsCount,
            long outOfStockVariantsCount
    ) {}

    public record CustomersKpi(
            long newCustomersToday,
            long totalActiveCustomers
    ) {}
}
