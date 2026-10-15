package com.techmart.core.bus.query.metrics;

import com.techmart.core.bus.Query;
import com.techmart.core.dto.response.metrics.StockAlertItemResponse;
import com.techmart.shared.dto.PagedResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public record GetStockAlertsQuery(
        UUID branchId,
        int threshold,
        Pageable pageable
) implements Query<PagedResponse<StockAlertItemResponse>> {}
