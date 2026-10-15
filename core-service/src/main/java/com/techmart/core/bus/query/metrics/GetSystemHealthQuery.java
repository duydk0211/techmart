package com.techmart.core.bus.query.metrics;

import com.techmart.core.bus.Query;
import com.techmart.core.dto.response.metrics.SystemHealthResponse;

public record GetSystemHealthQuery() implements Query<SystemHealthResponse> {}
