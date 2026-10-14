package com.techmart.core.controller.catalog.impl;

import com.techmart.core.bus.SpringBus;
import com.techmart.core.bus.command.catalog.CreateBannerCommand;
import com.techmart.core.bus.command.catalog.DeleteBannerCommand;
import com.techmart.core.bus.query.catalog.GetActiveBannersQuery;
import com.techmart.core.controller.catalog.InternalBannerApiController;
import com.techmart.core.dto.request.catalog.BannerRequest;
import com.techmart.core.dto.response.catalog.BannerResponse;
import com.techmart.shared.security.TechmartPreauthorize;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/internal/api/v1/banners")
public class InternalBannerApiControllerImpl implements InternalBannerApiController {

    private static final Logger log = LoggerFactory.getLogger(InternalBannerApiControllerImpl.class);

    private final SpringBus springBus;

    public InternalBannerApiControllerImpl(SpringBus springBus) {
        this.springBus = springBus;
    }

    @Override
    @GetMapping
    @TechmartPreauthorize(gateway = "TECHMART_GW", authenticated = false)
    public ResponseEntity<List<BannerResponse>> getActiveBanners() {
        log.info("[InternalBannerApiControllerImpl][getActiveBanners] Fetching active banners");
        List<BannerResponse> banners = springBus.query(new GetActiveBannersQuery());
        return ResponseEntity.ok(banners);
    }

    @Override
    @PostMapping
    @TechmartPreauthorize(gateway = "TECHMART_GW", roles = {"STAFF", "ADMIN"})
    public ResponseEntity<BannerResponse> createBanner(@Valid @RequestBody BannerRequest request) {
        log.info("[InternalBannerApiControllerImpl][createBanner] Creating banner: {}", request.title());
        BannerResponse response = springBus.execute(new CreateBannerCommand(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Override
    @DeleteMapping("/{id}")
    @TechmartPreauthorize(gateway = "TECHMART_GW", roles = {"ADMIN"})
    public ResponseEntity<Void> deleteBanner(@PathVariable("id") UUID id) {
        log.info("[InternalBannerApiControllerImpl][deleteBanner] Deleting banner: {}", id);
        springBus.execute(new DeleteBannerCommand(id));
        return ResponseEntity.noContent().build();
    }
}
