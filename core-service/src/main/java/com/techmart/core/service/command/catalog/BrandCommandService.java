package com.techmart.core.service.command.catalog;

import com.techmart.core.dto.request.catalog.BrandRequest;
import com.techmart.core.dto.response.catalog.BrandResponse;

public interface BrandCommandService {

    BrandResponse createBrand(BrandRequest request);
}
