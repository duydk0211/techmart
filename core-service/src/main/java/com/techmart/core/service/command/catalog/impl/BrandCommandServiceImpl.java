package com.techmart.core.service.command.catalog.impl;

import com.techmart.core.dto.request.catalog.BrandRequest;
import com.techmart.core.dto.response.catalog.BrandResponse;
import com.techmart.core.entity.catalog.BrandEntity;
import com.techmart.core.repository.catalog.BrandRepository;
import com.techmart.core.service.command.catalog.BrandCommandService;
import com.techmart.shared.constant.ResponseCode;
import com.techmart.shared.exception.CoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class BrandCommandServiceImpl implements BrandCommandService {

    private static final Logger log = LoggerFactory.getLogger(BrandCommandServiceImpl.class);

    private final BrandRepository brandRepository;

    public BrandCommandServiceImpl(BrandRepository brandRepository) {
        this.brandRepository = brandRepository;
    }

    @Override
    public BrandResponse createBrand(BrandRequest request) {
        log.info("[BrandCommandServiceImpl][createBrand] Creating brand: {}", request.name());

        if (brandRepository.existsByNameIgnoreCase(request.name())) {
            throw new CoreException(ResponseCode.DUPLICATE_RESOURCE, "Brand with name already exists: " + request.name());
        }

        BrandEntity entity = new BrandEntity();
        entity.setName(request.name());
        entity.setDescription(request.description());
        entity.setLogoUrl(request.logoUrl());
        entity.setActive(true);

        BrandEntity saved = brandRepository.save(entity);
        log.info("[BrandCommandServiceImpl][createBrand] Created brand id: {}", saved.getId());
        return BrandResponse.fromEntity(saved);
    }
}
