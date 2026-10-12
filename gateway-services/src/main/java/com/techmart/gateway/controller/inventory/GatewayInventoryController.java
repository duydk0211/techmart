package com.techmart.gateway.controller.inventory;

import com.techmart.gateway.client.CoreClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/inventory")
public class GatewayInventoryController {

    private static final Logger log = LoggerFactory.getLogger(GatewayInventoryController.class);

    private final CoreClient coreClient;

    public GatewayInventoryController(CoreClient coreClient) {
        this.coreClient = coreClient;
    }

    @PostMapping("/adjust")
    public ResponseEntity<Object> adjustInventory(@RequestBody Object body) {
        log.info("[GatewayInventoryController][adjustInventory] Proxying adjustInventory to core-service");
        return coreClient.forward(HttpMethod.POST, "/internal/api/v1/inventory/adjust", body, Object.class);
    }
}
