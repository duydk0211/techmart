package com.techmart.core.service.query.catalog;

import com.techmart.core.dto.response.catalog.BannerResponse;

import java.util.List;

public interface BannerQueryService {
    List<BannerResponse> getActiveBanners();
}
