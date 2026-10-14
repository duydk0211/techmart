package com.techmart.core.controller.catalog;

import com.techmart.core.dto.request.catalog.BannerRequest;
import com.techmart.core.dto.response.catalog.BannerResponse;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

public interface InternalBannerApiController {

    ResponseEntity<List<BannerResponse>> getActiveBanners();

    ResponseEntity<BannerResponse> createBanner(BannerRequest request);

    ResponseEntity<Void> deleteBanner(UUID id);
}
