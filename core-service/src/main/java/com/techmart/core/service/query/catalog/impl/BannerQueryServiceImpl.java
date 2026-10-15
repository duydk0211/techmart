package com.techmart.core.service.query.catalog.impl;

import com.techmart.core.dto.response.catalog.BannerResponse;
import com.techmart.core.entity.catalog.BannerEntity;
import com.techmart.core.repository.catalog.BannerRepository;
import com.techmart.core.service.query.catalog.BannerQueryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class BannerQueryServiceImpl implements BannerQueryService {

    private static final Logger log = LoggerFactory.getLogger(BannerQueryServiceImpl.class);

    private final BannerRepository bannerRepository;

    public BannerQueryServiceImpl(BannerRepository bannerRepository) {
        this.bannerRepository = bannerRepository;
    }

    @Override
    public List<BannerResponse> getActiveBanners() {
        log.info("[BannerQueryServiceImpl][getActiveBanners] Querying active banners ordered by displayOrder ASC, createdAt DESC");
        List<BannerEntity> banners = bannerRepository.findAllByActiveTrueOrderByDisplayOrderAscCreatedAtDesc();
        return banners.stream()
                .map(BannerResponse::fromEntity)
                .toList();
    }
}
