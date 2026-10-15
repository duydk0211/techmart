package com.techmart.core.controller.metrics;

import com.techmart.core.dto.response.metrics.DashboardSummaryResponse;
import com.techmart.core.dto.response.metrics.RevenueChartResponse;
import com.techmart.core.dto.response.metrics.StockAlertItemResponse;
import com.techmart.core.dto.response.metrics.SystemHealthResponse;
import com.techmart.shared.dto.PagedResponse;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

public interface InternalMetricsApiController {

    ResponseEntity<DashboardSummaryResponse> getSummary(UUID branchId);

    ResponseEntity<RevenueChartResponse> getRevenueChart(String period, UUID branchId);

    ResponseEntity<PagedResponse<StockAlertItemResponse>> getStockAlerts(UUID branchId, int threshold, int page, int size);

    ResponseEntity<SystemHealthResponse> getSystemHealth();
}
