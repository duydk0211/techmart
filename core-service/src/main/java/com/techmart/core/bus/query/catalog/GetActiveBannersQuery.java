package com.techmart.core.bus.query.catalog;

import com.techmart.core.bus.Query;
import com.techmart.core.dto.response.catalog.BannerResponse;

import java.util.List;

public record GetActiveBannersQuery() implements Query<List<BannerResponse>> {
}
