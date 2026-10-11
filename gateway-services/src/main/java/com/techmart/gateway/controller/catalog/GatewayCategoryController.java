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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/categories")
public class GatewayCategoryController {

    private static final Logger log = LoggerFactory.getLogger(GatewayCategoryController.class);

    private final CoreClient coreClient;

    public GatewayCategoryController(CoreClient coreClient) {
        this.coreClient = coreClient;
    }

    @GetMapping("/tree")
    public ResponseEntity<Object> getCategoryTree() {
        log.info("[GatewayCategoryController][getCategoryTree] Proxying getCategoryTree to core-service");
        return coreClient.forward(HttpMethod.GET, "/internal/api/v1/categories/tree", null, Object.class);
    }

    @PostMapping
    public ResponseEntity<Object> createCategory(@RequestBody Object body) {
        log.info("[GatewayCategoryController][createCategory] Proxying createCategory to core-service");
        return coreClient.forward(HttpMethod.POST, "/internal/api/v1/categories", body, Object.class);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Object> updateCategory(@PathVariable("id") UUID id, @RequestBody Object body) {
        log.info("[GatewayCategoryController][updateCategory] Proxying updateCategory to core-service: {}", id);
        return coreClient.forward(HttpMethod.PUT, "/internal/api/v1/categories/" + id, body, Object.class);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Object> deleteCategory(@PathVariable("id") UUID id) {
        log.info("[GatewayCategoryController][deleteCategory] Proxying deleteCategory to core-service: {}", id);
        return coreClient.forward(HttpMethod.DELETE, "/internal/api/v1/categories/" + id, null, Object.class);
    }
}
