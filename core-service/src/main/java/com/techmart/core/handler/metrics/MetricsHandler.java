package com.techmart.core.handler.metrics;

import com.techmart.core.bus.QueryHandler;
import com.techmart.core.bus.query.metrics.GetDashboardSummaryQuery;
import com.techmart.core.bus.query.metrics.GetRevenueChartQuery;
import com.techmart.core.bus.query.metrics.GetStockAlertsQuery;
import com.techmart.core.bus.query.metrics.GetSystemHealthQuery;
import com.techmart.core.dto.response.metrics.DashboardSummaryResponse;
import com.techmart.core.dto.response.metrics.RevenueChartResponse;
import com.techmart.core.dto.response.metrics.StockAlertItemResponse;
import com.techmart.core.dto.response.metrics.SystemHealthResponse;
import com.techmart.core.service.query.metrics.MetricsQueryService;
import com.techmart.shared.dto.PagedResponse;
import org.springframework.stereotype.Component;

public class MetricsHandler {

    @Component
    public static class GetDashboardSummaryQueryHandler implements QueryHandler<GetDashboardSummaryQuery, DashboardSummaryResponse> {
        private final MetricsQueryService metricsQueryService;

        public GetDashboardSummaryQueryHandler(MetricsQueryService metricsQueryService) {
            this.metricsQueryService = metricsQueryService;
        }

        @Override
        public DashboardSummaryResponse handle(GetDashboardSummaryQuery query) {
            return metricsQueryService.getDashboardSummary(query.branchId());
        }
    }

    @Component
    public static class GetRevenueChartQueryHandler implements QueryHandler<GetRevenueChartQuery, RevenueChartResponse> {
        private final MetricsQueryService metricsQueryService;

        public GetRevenueChartQueryHandler(MetricsQueryService metricsQueryService) {
            this.metricsQueryService = metricsQueryService;
        }

        @Override
        public RevenueChartResponse handle(GetRevenueChartQuery query) {
            return metricsQueryService.getRevenueChart(query.period(), query.branchId());
        }
    }

    @Component
    public static class GetStockAlertsQueryHandler implements QueryHandler<GetStockAlertsQuery, PagedResponse<StockAlertItemResponse>> {
        private final MetricsQueryService metricsQueryService;

        public GetStockAlertsQueryHandler(MetricsQueryService metricsQueryService) {
            this.metricsQueryService = metricsQueryService;
        }

        @Override
        public PagedResponse<StockAlertItemResponse> handle(GetStockAlertsQuery query) {
            return metricsQueryService.getStockAlerts(query.branchId(), query.threshold(), query.pageable());
        }
    }

    @Component
    public static class GetSystemHealthQueryHandler implements QueryHandler<GetSystemHealthQuery, SystemHealthResponse> {
        private final MetricsQueryService metricsQueryService;

        public GetSystemHealthQueryHandler(MetricsQueryService metricsQueryService) {
            this.metricsQueryService = metricsQueryService;
        }

        @Override
        public SystemHealthResponse handle(GetSystemHealthQuery query) {
            return metricsQueryService.getSystemHealth();
        }
    }
}
