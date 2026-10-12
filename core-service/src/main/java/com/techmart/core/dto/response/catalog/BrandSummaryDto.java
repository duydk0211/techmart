package com.techmart.core.dto.response.catalog;

import java.util.UUID;

public record BrandSummaryDto(
        UUID id,
        String name,
        String logoUrl
) {
    public BrandSummaryDto(UUID id, String name) {
        this(id, name, null);
    }
}
