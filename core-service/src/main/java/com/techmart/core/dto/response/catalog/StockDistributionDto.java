package com.techmart.core.dto.response.catalog;

import java.util.UUID;

public record StockDistributionDto(
        UUID branchId,
        String branchName,
        String branchAddress,
        int availableQuantity
) {
}
