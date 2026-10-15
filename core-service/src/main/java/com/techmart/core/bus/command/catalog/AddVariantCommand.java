package com.techmart.core.bus.command.catalog;

import com.techmart.core.bus.Command;
import com.techmart.core.dto.request.catalog.VariantCreateRequest;
import com.techmart.core.dto.response.catalog.VariantDetailResponse;

import java.util.UUID;

public record AddVariantCommand(
        UUID productId,
        VariantCreateRequest request
) implements Command<VariantDetailResponse> {
}
