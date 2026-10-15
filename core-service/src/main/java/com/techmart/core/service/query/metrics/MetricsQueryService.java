package com.techmart.core.service.query.metrics;

import com.techmart.core.dto.response.metrics.DashboardSummaryResponse;
import com.techmart.core.dto.response.metrics.RevenueChartResponse;
import com.techmart.core.dto.response.metrics.StockAlertItemResponse;
import com.techmart.core.dto.response.metrics.SystemHealthResponse;
import com.techmart.shared.dto.PagedResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface MetricsQueryService {

    DashboardSummaryResponse getDashboardSummary(UUID branchId);

    RevenueChartResponse getRevenueChart(String period, UUID branchId);

    PagedResponse<StockAlertItemResponse> getStockAlerts(UUID branchId, int threshold, Pageable pageable);

    SystemHealthResponse getSystemHealth();
}
