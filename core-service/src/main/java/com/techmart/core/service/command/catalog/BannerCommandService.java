package com.techmart.core.service.command.catalog;

import com.techmart.core.dto.request.catalog.BannerRequest;
import com.techmart.core.dto.response.catalog.BannerResponse;

import java.util.UUID;

public interface BannerCommandService {
    BannerResponse createBanner(BannerRequest request);
    void deleteBanner(UUID id);
}
