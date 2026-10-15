package com.techmart.gateway.controller.catalog;

import com.techmart.gateway.client.CoreClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/banners")
public class GatewayBannerController {

    private static final Logger log = LoggerFactory.getLogger(GatewayBannerController.class);

    private final CoreClient coreClient;

    public GatewayBannerController(CoreClient coreClient) {
        this.coreClient = coreClient;
    }

    @GetMapping
    public ResponseEntity<Object> getActiveBanners() {
        log.info("[GatewayBannerController][getActiveBanners] Proxying getActiveBanners to core-service");
        return coreClient.forward(HttpMethod.GET, "/internal/api/v1/banners", null, Object.class);
    }

    @PostMapping
    public ResponseEntity<Object> createBanner(@RequestBody Object body) {
        log.info("[GatewayBannerController][createBanner] Proxying createBanner to core-service");
        return coreClient.forward(HttpMethod.POST, "/internal/api/v1/banners", body, Object.class);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Object> deleteBanner(@PathVariable UUID id) {
        log.info("[GatewayBannerController][deleteBanner] Proxying deleteBanner to core-service: {}", id);
        return coreClient.forward(HttpMethod.DELETE, "/internal/api/v1/banners/" + id, null, Object.class);
    }
}
