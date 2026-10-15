package com.techmart.core.controller.catalog.impl;

import com.techmart.core.bus.SpringBus;
import com.techmart.core.bus.command.catalog.CreateBrandCommand;
import com.techmart.core.bus.query.catalog.GetBrandListQuery;
import com.techmart.core.controller.catalog.InternalBrandApiController;
import com.techmart.core.dto.request.catalog.BrandRequest;
import com.techmart.core.dto.response.catalog.BrandResponse;
import com.techmart.shared.security.TechmartPreauthorize;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/internal/api/v1/brands")
public class InternalBrandApiControllerImpl implements InternalBrandApiController {

    private static final Logger log = LoggerFactory.getLogger(InternalBrandApiControllerImpl.class);

    private final SpringBus springBus;

    public InternalBrandApiControllerImpl(SpringBus springBus) {
        this.springBus = springBus;
    }

    @Override
    @GetMapping
    @TechmartPreauthorize(gateway = "TECHMART_GW", authenticated = false)
    public ResponseEntity<List<BrandResponse>> getBrandList() {
        log.info("[InternalBrandApiControllerImpl][getBrandList] Fetching brand list");
        List<BrandResponse> brands = springBus.query(new GetBrandListQuery());
        return ResponseEntity.ok(brands);
    }

    @Override
    @PostMapping
    @TechmartPreauthorize(gateway = "TECHMART_GW", roles = {"ADMIN"})
    public ResponseEntity<BrandResponse> createBrand(@Valid @RequestBody BrandRequest request) {
        log.info("[InternalBrandApiControllerImpl][createBrand] Creating brand: {}", request.name());
        BrandResponse response = springBus.execute(new CreateBrandCommand(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
