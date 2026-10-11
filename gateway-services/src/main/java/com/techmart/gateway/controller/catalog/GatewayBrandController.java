package com.techmart.gateway.controller.catalog;

import com.techmart.gateway.client.CoreClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/brands")
public class GatewayBrandController {

    private static final Logger log = LoggerFactory.getLogger(GatewayBrandController.class);

    private final CoreClient coreClient;

    public GatewayBrandController(CoreClient coreClient) {
        this.coreClient = coreClient;
    }

    @GetMapping
    public ResponseEntity<Object> getBrandList() {
        log.info("[GatewayBrandController][getBrandList] Proxying getBrandList to core-service");
        return coreClient.forward(HttpMethod.GET, "/internal/api/v1/brands", null, Object.class);
    }

    @PostMapping
    public ResponseEntity<Object> createBrand(@RequestBody Object body) {
        log.info("[GatewayBrandController][createBrand] Proxying createBrand to core-service");
        return coreClient.forward(HttpMethod.POST, "/internal/api/v1/brands", body, Object.class);
    }
}
