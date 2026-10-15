package com.techmart.core.dto.response.catalog;

import java.util.UUID;

public record CategorySummaryDto(
        UUID id,
        String name
) {
}
