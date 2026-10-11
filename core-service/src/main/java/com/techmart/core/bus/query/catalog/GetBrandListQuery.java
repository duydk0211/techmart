package com.techmart.core.bus.query.catalog;

import com.techmart.core.bus.Query;
import com.techmart.core.dto.response.catalog.BrandResponse;

import java.util.List;

public record GetBrandListQuery() implements Query<List<BrandResponse>> {
}
