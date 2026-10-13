package com.techmart.core.dto.response.metrics;

/**
 * Real-time operational infrastructure health and metrics response.
 */
public record SystemHealthResponse(
        ServerMetrics server,
        MemoryMetrics memory,
        DatabasePoolMetrics databasePool,
        CacheMetrics cache,
        IdempotencyMetrics idempotencyMetrics
) {
    public record ServerMetrics(
            long uptimeSeconds,
            String uptimeHuman
    ) {}

    public record MemoryMetrics(
            long heapUsedMb,
            long heapMaxMb,
            double heapUsagePercent
    ) {}

    public record DatabasePoolMetrics(
            int activeConnections,
            int idleConnections,
            int totalConnections,
            int threadsAwaitingConnection
    ) {}

    public record CacheMetrics(
            String status
    ) {}

    public record IdempotencyMetrics(
            long totalBlockedRequests
    ) {}
}
