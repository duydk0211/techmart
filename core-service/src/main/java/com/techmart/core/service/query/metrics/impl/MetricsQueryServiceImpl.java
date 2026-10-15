package com.techmart.core.service.query.metrics.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.techmart.core.dto.response.metrics.DashboardSummaryResponse;
import com.techmart.core.dto.response.metrics.RevenueChartResponse;
import com.techmart.core.dto.response.metrics.StockAlertItemResponse;
import com.techmart.core.dto.response.metrics.SystemHealthResponse;
import com.techmart.core.service.command.idempotency.IdempotencyService;
import com.techmart.core.service.query.metrics.MetricsQueryService;
import com.techmart.shared.dto.PagedResponse;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.RuntimeMXBean;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class MetricsQueryServiceImpl implements MetricsQueryService {

    private static final Logger log = LoggerFactory.getLogger(MetricsQueryServiceImpl.class);

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final DataSource dataSource;
    private final IdempotencyService idempotencyService;
    private final ObjectMapper objectMapper;

    public MetricsQueryServiceImpl(
            NamedParameterJdbcTemplate jdbcTemplate,
            DataSource dataSource,
            IdempotencyService idempotencyService,
            ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.dataSource = dataSource;
        this.idempotencyService = idempotencyService;
        this.objectMapper = objectMapper;
    }

    @Override
    public DashboardSummaryResponse getDashboardSummary(UUID branchId) {
        log.info("[MetricsQueryServiceImpl][getDashboardSummary] Aggregating summary KPIs for branch: {}", branchId);

        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        Instant todayStart = today.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant todayEnd = today.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant yesterdayStart = today.minusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        Timestamp tsTodayStart = Timestamp.from(todayStart);
        Timestamp tsTodayEnd = Timestamp.from(todayEnd);
        Timestamp tsYesterdayStart = Timestamp.from(yesterdayStart);

        String orderSql = """
            SELECT
                COALESCE(SUM(CASE WHEN created_at >= :todayStart AND created_at < :todayEnd AND status = 'DELIVERED' THEN total_amount ELSE 0 END), 0) AS today_revenue,
                COALESCE(SUM(CASE WHEN created_at >= :yesterdayStart AND created_at < :todayStart AND status = 'DELIVERED' THEN total_amount ELSE 0 END), 0) AS yesterday_revenue,
                COUNT(CASE WHEN created_at >= :todayStart AND created_at < :todayEnd THEN 1 END) AS today_orders_total,
                COUNT(CASE WHEN created_at >= :todayStart AND created_at < :todayEnd AND status = 'PENDING' THEN 1 END) AS pending_count,
                COUNT(CASE WHEN created_at >= :todayStart AND created_at < :todayEnd AND status = 'CONFIRMED' THEN 1 END) AS confirmed_count,
                COUNT(CASE WHEN created_at >= :todayStart AND created_at < :todayEnd AND status = 'PROCESSING' THEN 1 END) AS processing_count,
                COUNT(CASE WHEN created_at >= :todayStart AND created_at < :todayEnd AND status = 'SHIPPED' THEN 1 END) AS shipped_count,
                COUNT(CASE WHEN created_at >= :todayStart AND created_at < :todayEnd AND status = 'DELIVERED' THEN 1 END) AS delivered_count,
                COUNT(CASE WHEN created_at >= :todayStart AND created_at < :todayEnd AND status = 'CANCELLED' THEN 1 END) AS cancelled_count
            FROM orders
            WHERE (:branchId IS NULL OR branch_id = :branchId)
        """;

        MapSqlParameterSource orderParams = new MapSqlParameterSource()
                .addValue("todayStart", tsTodayStart)
                .addValue("todayEnd", tsTodayEnd)
                .addValue("yesterdayStart", tsYesterdayStart)
                .addValue("branchId", branchId);

        DashboardSummaryResponse.RevenueKpi revenueKpi = jdbcTemplate.query(orderSql, orderParams, rs -> {
            if (rs.next()) {
                BigDecimal todayRev = rs.getBigDecimal("today_revenue");
                BigDecimal yesterdayRev = rs.getBigDecimal("yesterday_revenue");
                Double growth;
                if (yesterdayRev == null || yesterdayRev.compareTo(BigDecimal.ZERO) == 0) {
                    growth = (todayRev != null && todayRev.compareTo(BigDecimal.ZERO) > 0) ? 100.0 : 0.0;
                } else {
                    growth = todayRev.subtract(yesterdayRev)
                            .divide(yesterdayRev, 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100))
                            .setScale(2, RoundingMode.HALF_UP)
                            .doubleValue();
                }
                return new DashboardSummaryResponse.RevenueKpi(todayRev, yesterdayRev, growth);
            }
            return new DashboardSummaryResponse.RevenueKpi(BigDecimal.ZERO, BigDecimal.ZERO, 0.0);
        });

        DashboardSummaryResponse.OrdersKpi ordersKpi = jdbcTemplate.query(orderSql, orderParams, rs -> {
            if (rs.next()) {
                return new DashboardSummaryResponse.OrdersKpi(
                        rs.getLong("today_orders_total"),
                        rs.getLong("pending_count"),
                        rs.getLong("confirmed_count"),
                        rs.getLong("processing_count"),
                        rs.getLong("shipped_count"),
                        rs.getLong("delivered_count"),
                        rs.getLong("cancelled_count")
                );
            }
            return new DashboardSummaryResponse.OrdersKpi(0, 0, 0, 0, 0, 0, 0);
        });

        String inventorySql = """
            SELECT
                COUNT(CASE WHEN (quantity_on_hand - quantity_reserved) <= reorder_level AND (quantity_on_hand - quantity_reserved) > 0 THEN 1 END) AS low_stock,
                COUNT(CASE WHEN (quantity_on_hand - quantity_reserved) <= 0 THEN 1 END) AS out_of_stock
            FROM inventory
            WHERE (:branchId IS NULL OR branch_id = :branchId)
        """;

        MapSqlParameterSource invParams = new MapSqlParameterSource().addValue("branchId", branchId);
        DashboardSummaryResponse.InventoryKpi inventoryKpi = jdbcTemplate.query(inventorySql, invParams, rs -> {
            if (rs.next()) {
                return new DashboardSummaryResponse.InventoryKpi(
                        rs.getLong("low_stock"),
                        rs.getLong("out_of_stock")
                );
            }
            return new DashboardSummaryResponse.InventoryKpi(0, 0);
        });

        String userSql = """
            SELECT
                COUNT(CASE WHEN created_at >= :todayStart AND role = 'CUSTOMER' THEN 1 END) AS new_customers,
                COUNT(CASE WHEN deleted = false AND role = 'CUSTOMER' THEN 1 END) AS active_customers
            FROM users
        """;
        MapSqlParameterSource userParams = new MapSqlParameterSource().addValue("todayStart", tsTodayStart);
        DashboardSummaryResponse.CustomersKpi customersKpi = jdbcTemplate.query(userSql, userParams, rs -> {
            if (rs.next()) {
                return new DashboardSummaryResponse.CustomersKpi(
                        rs.getLong("new_customers"),
                        rs.getLong("active_customers")
                );
            }
            return new DashboardSummaryResponse.CustomersKpi(0, 0);
        });

        return new DashboardSummaryResponse(revenueKpi, ordersKpi, inventoryKpi, customersKpi, Instant.now());
    }

    @Override
    public RevenueChartResponse getRevenueChart(String period, UUID branchId) {
        log.info("[MetricsQueryServiceImpl][getRevenueChart] Building revenue timeline for period: {}, branch: {}", period, branchId);

        LocalDate endDate = LocalDate.now(ZoneOffset.UTC);
        LocalDate startDate;
        String normalizedPeriod = period != null ? period.toUpperCase() : "7_DAYS";

        switch (normalizedPeriod) {
            case "TODAY" -> startDate = endDate;
            case "30_DAYS" -> startDate = endDate.minusDays(29);
            case "THIS_MONTH" -> startDate = endDate.withDayOfMonth(1);
            case "THIS_YEAR" -> startDate = endDate.withDayOfYear(1);
            case "7_DAYS" -> startDate = endDate.minusDays(6);
            default -> {
                normalizedPeriod = "7_DAYS";
                startDate = endDate.minusDays(6);
            }
        }

        Timestamp startTs = Timestamp.from(startDate.atStartOfDay(ZoneOffset.UTC).toInstant());
        Timestamp endTs = Timestamp.from(endDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant());

        String chartSql = """
            SELECT CAST(created_at AS DATE) AS order_date,
                   COALESCE(SUM(total_amount), 0) AS day_revenue,
                   COUNT(*) AS day_orders
            FROM orders
            WHERE created_at >= :startTs AND created_at < :endTs
              AND status != 'CANCELLED'
              AND (:branchId IS NULL OR branch_id = :branchId)
            GROUP BY CAST(created_at AS DATE)
            ORDER BY order_date ASC
        """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("startTs", startTs)
                .addValue("endTs", endTs)
                .addValue("branchId", branchId);

        record DayStat(BigDecimal revenue, long orders) {}
        Map<LocalDate, DayStat> dbMap = new HashMap<>();

        jdbcTemplate.query(chartSql, params, rs -> {
            Date d = rs.getDate("order_date");
            if (d != null) {
                dbMap.put(d.toLocalDate(), new DayStat(rs.getBigDecimal("day_revenue"), rs.getLong("day_orders")));
            }
        });

        List<RevenueChartResponse.TimelinePoint> timeline = new ArrayList<>();
        BigDecimal totalRevenue = BigDecimal.ZERO;
        long totalOrders = 0;

        LocalDate cur = startDate;
        while (!cur.isAfter(endDate)) {
            DayStat stat = dbMap.getOrDefault(cur, new DayStat(BigDecimal.ZERO, 0L));
            timeline.add(new RevenueChartResponse.TimelinePoint(cur.toString(), stat.revenue(), stat.orders()));
            totalRevenue = totalRevenue.add(stat.revenue());
            totalOrders += stat.orders();
            cur = cur.plusDays(1);
        }

        return new RevenueChartResponse(normalizedPeriod, timeline, totalRevenue, totalOrders);
    }

    @Override
    public PagedResponse<StockAlertItemResponse> getStockAlerts(UUID branchId, int threshold, Pageable pageable) {
        int effectiveThreshold = threshold > 0 ? threshold : 5;
        log.info("[MetricsQueryServiceImpl][getStockAlerts] Querying stock alerts for branch: {}, threshold: {}, page: {}",
                branchId, effectiveThreshold, pageable.getPageNumber());

        String countSql = """
            SELECT COUNT(*)
            FROM inventory i
            JOIN product_variants pv ON i.variant_id = pv.id
            JOIN products p ON pv.product_id = p.id
            JOIN branches b ON i.branch_id = b.id
            WHERE (:branchId IS NULL OR i.branch_id = :branchId)
              AND ((i.quantity_on_hand - i.quantity_reserved) <= :threshold
                   OR (i.quantity_on_hand - i.quantity_reserved) <= i.reorder_level)
        """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("branchId", branchId)
                .addValue("threshold", effectiveThreshold)
                .addValue("limit", pageable.getPageSize())
                .addValue("offset", pageable.getOffset());

        Long totalCount = jdbcTemplate.queryForObject(countSql, params, Long.class);
        long totalElements = totalCount != null ? totalCount : 0L;

        if (totalElements == 0) {
            return PagedResponse.of(List.of(), pageable.getPageNumber(), pageable.getPageSize(), 0);
        }

        String querySql = """
            SELECT i.branch_id, b.name AS branch_name,
                   p.id AS product_id, p.name AS product_name,
                   pv.id AS variant_id, pv.sku, pv.attributes,
                   i.quantity_on_hand, i.quantity_reserved,
                   (i.quantity_on_hand - i.quantity_reserved) AS quantity_available,
                   i.reorder_level
            FROM inventory i
            JOIN product_variants pv ON i.variant_id = pv.id
            JOIN products p ON pv.product_id = p.id
            JOIN branches b ON i.branch_id = b.id
            WHERE (:branchId IS NULL OR i.branch_id = :branchId)
              AND ((i.quantity_on_hand - i.quantity_reserved) <= :threshold
                   OR (i.quantity_on_hand - i.quantity_reserved) <= i.reorder_level)
            ORDER BY (i.quantity_on_hand - i.quantity_reserved) ASC, pv.sku ASC
            LIMIT :limit OFFSET :offset
        """;

        List<StockAlertItemResponse> items = jdbcTemplate.query(querySql, params, (rs, rowNum) -> {
            int available = rs.getInt("quantity_available");
            String alertStatus;
            if (available <= 0) {
                alertStatus = "OUT_OF_STOCK";
            } else if (available <= 2) {
                alertStatus = "CRITICAL_LOW";
            } else {
                alertStatus = "LOW_STOCK";
            }

            String attrJson = rs.getString("attributes");
            Map<String, Object> attributes = Map.of();
            if (attrJson != null && !attrJson.isBlank()) {
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> parsed = objectMapper.readValue(attrJson, Map.class);
                    attributes = parsed;
                } catch (Exception ignored) {
                    // Fallback to empty attributes on parse failure
                }
            }

            return new StockAlertItemResponse(
                    UUID.fromString(rs.getString("branch_id")),
                    rs.getString("branch_name"),
                    UUID.fromString(rs.getString("product_id")),
                    rs.getString("product_name"),
                    UUID.fromString(rs.getString("variant_id")),
                    rs.getString("sku"),
                    attributes,
                    rs.getInt("quantity_on_hand"),
                    rs.getInt("quantity_reserved"),
                    available,
                    rs.getInt("reorder_level"),
                    alertStatus
            );
        });

        return PagedResponse.of(items, pageable.getPageNumber(), pageable.getPageSize(), totalElements);
    }

    @Override
    public SystemHealthResponse getSystemHealth() {
        log.info("[MetricsQueryServiceImpl][getSystemHealth] Inspecting real-time system health metrics");

        RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        long uptimeSeconds = runtimeMXBean.getUptime() / 1000;
        long days = uptimeSeconds / 86400;
        long hours = (uptimeSeconds % 86400) / 3600;
        long minutes = (uptimeSeconds % 3600) / 60;
        long seconds = uptimeSeconds % 60;
        String uptimeHuman = String.format("%dd %dh %dm %ds", days, hours, minutes, seconds);
        SystemHealthResponse.ServerMetrics serverMetrics = new SystemHealthResponse.ServerMetrics(uptimeSeconds, uptimeHuman);

        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapUsage = memoryMXBean.getHeapMemoryUsage();
        long usedMb = heapUsage.getUsed() / (1024 * 1024);
        long maxMb = heapUsage.getMax() > 0 ? heapUsage.getMax() / (1024 * 1024) : usedMb;
        double heapPercent = maxMb > 0 ? Math.round(((double) heapUsage.getUsed() / heapUsage.getMax()) * 10000.0) / 100.0 : 0.0;
        SystemHealthResponse.MemoryMetrics memoryMetrics = new SystemHealthResponse.MemoryMetrics(usedMb, maxMb, heapPercent);

        int active = 0;
        int idle = 0;
        int total = 0;
        int awaiting = 0;
        if (dataSource instanceof HikariDataSource hikari) {
            HikariPoolMXBean poolBean = hikari.getHikariPoolMXBean();
            if (poolBean != null) {
                active = poolBean.getActiveConnections();
                idle = poolBean.getIdleConnections();
                total = poolBean.getTotalConnections();
                awaiting = poolBean.getThreadsAwaitingConnection();
            }
        }
        SystemHealthResponse.DatabasePoolMetrics poolMetrics = new SystemHealthResponse.DatabasePoolMetrics(active, idle, total, awaiting);
        SystemHealthResponse.CacheMetrics cacheMetrics = new SystemHealthResponse.CacheMetrics("UP");
        long blockedCount = idempotencyService.getBlockedRequestsCount();
        SystemHealthResponse.IdempotencyMetrics idempotencyMetrics = new SystemHealthResponse.IdempotencyMetrics(blockedCount);

        return new SystemHealthResponse(serverMetrics, memoryMetrics, poolMetrics, cacheMetrics, idempotencyMetrics);
    }
}
