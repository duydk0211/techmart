package com.techmart.core.dto.response.catalog;

import java.math.BigDecimal;

public record PriceRangeDto(
        BigDecimal min,
        BigDecimal max,
        String currency
) {
    public PriceRangeDto(BigDecimal min, BigDecimal max) {
        this(min, max, "VND");
    }
}
