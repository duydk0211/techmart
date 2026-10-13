package com.techmart.core.controller.metrics.impl;

import com.techmart.core.bus.SpringBus;
import com.techmart.core.bus.query.metrics.GetDashboardSummaryQuery;
import com.techmart.core.bus.query.metrics.GetRevenueChartQuery;
import com.techmart.core.bus.query.metrics.GetStockAlertsQuery;
import com.techmart.core.bus.query.metrics.GetSystemHealthQuery;
import com.techmart.core.controller.metrics.InternalMetricsApiController;
import com.techmart.core.dto.response.metrics.DashboardSummaryResponse;
import com.techmart.core.dto.response.metrics.RevenueChartResponse;
import com.techmart.core.dto.response.metrics.StockAlertItemResponse;
import com.techmart.core.dto.response.metrics.SystemHealthResponse;
import com.techmart.shared.dto.PagedResponse;
import com.techmart.shared.security.TechmartPreauthorize;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/internal/api/v1/metrics")
public class InternalMetricsApiControllerImpl implements InternalMetricsApiController {

    private static final Logger log = LoggerFactory.getLogger(InternalMetricsApiControllerImpl.class);

    private final SpringBus springBus;

    public InternalMetricsApiControllerImpl(SpringBus springBus) {
        this.springBus = springBus;
    }

    @Override
    @GetMapping("/summary")
    @TechmartPreauthorize(gateway = "TECHMART_GW", roles = {"STAFF", "ADMIN"})
    public ResponseEntity<DashboardSummaryResponse> getSummary(
            @RequestParam(required = false) UUID branchId) {
        log.info("[InternalMetricsApiControllerImpl][getSummary] Querying dashboard summary for branch: {}", branchId);
        DashboardSummaryResponse response = springBus.query(new GetDashboardSummaryQuery(branchId));
        return ResponseEntity.ok(response);
    }

    @Override
    @GetMapping("/revenue-chart")
    @TechmartPreauthorize(gateway = "TECHMART_GW", roles = {"STAFF", "ADMIN"})
    public ResponseEntity<RevenueChartResponse> getRevenueChart(
            @RequestParam(defaultValue = "7_DAYS") String period,
            @RequestParam(required = false) UUID branchId) {
        log.info("[InternalMetricsApiControllerImpl][getRevenueChart] Querying revenue chart for period: {}, branch: {}", period, branchId);
        RevenueChartResponse response = springBus.query(new GetRevenueChartQuery(period, branchId));
        return ResponseEntity.ok(response);
    }

    @Override
    @GetMapping("/stock-alerts")
    @TechmartPreauthorize(gateway = "TECHMART_GW", roles = {"STAFF", "ADMIN"})
    public ResponseEntity<PagedResponse<StockAlertItemResponse>> getStockAlerts(
            @RequestParam(required = false) UUID branchId,
            @RequestParam(defaultValue = "5") int threshold,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("[InternalMetricsApiControllerImpl][getStockAlerts] Querying stock alerts for branch: {}, threshold: {}, page: {}, size: {}",
                branchId, threshold, page, size);
        PagedResponse<StockAlertItemResponse> response = springBus.query(
                new GetStockAlertsQuery(branchId, threshold, PageRequest.of(page, size))
        );
        return ResponseEntity.ok(response);
    }

    @Override
    @GetMapping("/system-health")
    @TechmartPreauthorize(gateway = "TECHMART_GW", roles = {"ADMIN"})
    public ResponseEntity<SystemHealthResponse> getSystemHealth() {
        log.info("[InternalMetricsApiControllerImpl][getSystemHealth] Querying real-time system health");
        SystemHealthResponse response = springBus.query(new GetSystemHealthQuery());
        return ResponseEntity.ok(response);
    }
}
