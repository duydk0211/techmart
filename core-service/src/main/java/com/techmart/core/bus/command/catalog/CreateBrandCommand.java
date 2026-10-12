package com.techmart.core.bus.command.catalog;

import com.techmart.core.bus.Command;
import com.techmart.core.dto.request.catalog.BrandRequest;
import com.techmart.core.dto.response.catalog.BrandResponse;

public record CreateBrandCommand(
        BrandRequest request
) implements Command<BrandResponse> {
}
