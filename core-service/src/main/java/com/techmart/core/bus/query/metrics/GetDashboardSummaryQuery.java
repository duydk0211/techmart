package com.techmart.core.bus.query.metrics;

import com.techmart.core.bus.Query;
import com.techmart.core.dto.response.metrics.DashboardSummaryResponse;

import java.util.UUID;

public record GetDashboardSummaryQuery(
        UUID branchId
) implements Query<DashboardSummaryResponse> {}
