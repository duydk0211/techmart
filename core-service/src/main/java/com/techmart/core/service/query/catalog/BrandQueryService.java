package com.techmart.core.service.query.catalog;

import com.techmart.core.dto.response.catalog.BrandResponse;

import java.util.List;

public interface BrandQueryService {

    List<BrandResponse> getBrandList();
}
