package com.techmart.core.service.query.catalog.impl;

import com.techmart.core.dto.response.catalog.BrandResponse;
import com.techmart.core.repository.catalog.BrandRepository;
import com.techmart.core.service.query.catalog.BrandQueryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class BrandQueryServiceImpl implements BrandQueryService {

    private static final Logger log = LoggerFactory.getLogger(BrandQueryServiceImpl.class);

    private final BrandRepository brandRepository;

    public BrandQueryServiceImpl(BrandRepository brandRepository) {
        this.brandRepository = brandRepository;
    }

    @Override
    public List<BrandResponse> getBrandList() {
        log.info("[BrandQueryServiceImpl][getBrandList] Fetching all active brands");
        return brandRepository.findAllByActiveTrueOrderByNameAsc()
                .stream()
                .map(BrandResponse::fromEntity)
                .toList();
    }
}
