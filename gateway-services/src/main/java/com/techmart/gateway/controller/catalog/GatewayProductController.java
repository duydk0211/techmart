package com.techmart.gateway.controller.catalog;

import com.techmart.gateway.client.CoreClient;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products")
public class GatewayProductController {

    private static final Logger log = LoggerFactory.getLogger(GatewayProductController.class);

    private final CoreClient coreClient;

    public GatewayProductController(CoreClient coreClient) {
        this.coreClient = coreClient;
    }

    @GetMapping
    public ResponseEntity<Object> searchProducts(HttpServletRequest request) {
        log.info("[GatewayProductController][searchProducts] Proxying searchProducts to core-service");
        String query = request.getQueryString();
        String uri = "/internal/api/v1/products" + (query != null ? "?" + query : "");
        return coreClient.forward(HttpMethod.GET, uri, null, Object.class);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Object> getProductDetail(@PathVariable("id") UUID id) {
        log.info("[GatewayProductController][getProductDetail] Proxying getProductDetail to core-service: {}", id);
        return coreClient.forward(HttpMethod.GET, "/internal/api/v1/products/" + id, null, Object.class);
    }

    @GetMapping("/slug/{slug}")
    public ResponseEntity<Object> getProductBySlug(@PathVariable("slug") String slug) {
        log.info("[GatewayProductController][getProductBySlug] Proxying getProductBySlug to core-service: {}", slug);
        return coreClient.forward(HttpMethod.GET, "/internal/api/v1/products/slug/" + slug, null, Object.class);
    }

    @PostMapping
    public ResponseEntity<Object> createProduct(@RequestBody Object body) {
        log.info("[GatewayProductController][createProduct] Proxying createProduct to core-service");
        return coreClient.forward(HttpMethod.POST, "/internal/api/v1/products", body, Object.class);
    }

    @PostMapping("/{id}/variants")
    public ResponseEntity<Object> addVariant(@PathVariable("id") UUID id, @RequestBody Object body) {
        log.info("[GatewayProductController][addVariant] Proxying addVariant to core-service: {}", id);
        return coreClient.forward(HttpMethod.POST, "/internal/api/v1/products/" + id + "/variants", body, Object.class);
    }
}
