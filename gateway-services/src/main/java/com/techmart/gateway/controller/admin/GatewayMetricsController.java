package com.techmart.gateway.controller.admin;

import com.techmart.gateway.client.CoreClient;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/metrics")
public class GatewayMetricsController {

    private static final Logger log = LoggerFactory.getLogger(GatewayMetricsController.class);

    private final CoreClient coreClient;

    public GatewayMetricsController(CoreClient coreClient) {
        this.coreClient = coreClient;
    }

    @GetMapping("/summary")
    public ResponseEntity<Object> getSummary(HttpServletRequest request) {
        log.info("[GatewayMetricsController][getSummary] Proxying getSummary to core-service");
        String query = request.getQueryString();
        String uri = "/internal/api/v1/metrics/summary" + (query != null ? "?" + query : "");
        return coreClient.forward(HttpMethod.GET, uri, null, Object.class);
    }

    @GetMapping("/revenue-chart")
    public ResponseEntity<Object> getRevenueChart(HttpServletRequest request) {
        log.info("[GatewayMetricsController][getRevenueChart] Proxying getRevenueChart to core-service");
        String query = request.getQueryString();
        String uri = "/internal/api/v1/metrics/revenue-chart" + (query != null ? "?" + query : "");
        return coreClient.forward(HttpMethod.GET, uri, null, Object.class);
    }

    @GetMapping("/stock-alerts")
    public ResponseEntity<Object> getStockAlerts(HttpServletRequest request) {
        log.info("[GatewayMetricsController][getStockAlerts] Proxying getStockAlerts to core-service");
        String query = request.getQueryString();
        String uri = "/internal/api/v1/metrics/stock-alerts" + (query != null ? "?" + query : "");
        return coreClient.forward(HttpMethod.GET, uri, null, Object.class);
    }

    @GetMapping("/system-health")
    public ResponseEntity<Object> getSystemHealth() {
        log.info("[GatewayMetricsController][getSystemHealth] Proxying getSystemHealth to core-service");
        return coreClient.forward(HttpMethod.GET, "/internal/api/v1/metrics/system-health", null, Object.class);
    }
}
