package com.techmart.core.service.command.catalog.impl;

import com.techmart.core.dto.request.catalog.BannerRequest;
import com.techmart.core.dto.response.catalog.BannerResponse;
import com.techmart.core.entity.catalog.BannerEntity;
import com.techmart.core.repository.catalog.BannerRepository;
import com.techmart.core.service.command.catalog.BannerCommandService;
import com.techmart.shared.constant.ResponseCode;
import com.techmart.shared.exception.CoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class BannerCommandServiceImpl implements BannerCommandService {

    private static final Logger log = LoggerFactory.getLogger(BannerCommandServiceImpl.class);

    private final BannerRepository bannerRepository;

    public BannerCommandServiceImpl(BannerRepository bannerRepository) {
        this.bannerRepository = bannerRepository;
    }

    @Override
    public BannerResponse createBanner(BannerRequest request) {
        log.info("[BannerCommandServiceImpl][createBanner] Creating banner with title: {}", request.title());
        BannerEntity banner = new BannerEntity();
        banner.setTitle(request.title());
        banner.setImageUrl(request.imageUrl());
        banner.setTargetUrl(request.targetUrl());
        banner.setDisplayOrder(request.displayOrder() != null ? request.displayOrder() : 0);
        banner.setActive(request.active() != null ? request.active() : true);

        BannerEntity saved = bannerRepository.save(banner);
        log.info("[BannerCommandServiceImpl][createBanner] Banner created successfully with id: {}", saved.getId());
        return BannerResponse.fromEntity(saved);
    }

    @Override
    public void deleteBanner(UUID id) {
        log.info("[BannerCommandServiceImpl][deleteBanner] Deleting banner with id: {}", id);
        BannerEntity banner = bannerRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("[BannerCommandServiceImpl][deleteBanner] Banner not found with id: {}", id);
                    return new CoreException(ResponseCode.RESOURCE_NOT_FOUND, "Banner not found with id: " + id);
                });

        bannerRepository.delete(banner);
        log.info("[BannerCommandServiceImpl][deleteBanner] Banner deleted successfully with id: {}", id);
    }
}
