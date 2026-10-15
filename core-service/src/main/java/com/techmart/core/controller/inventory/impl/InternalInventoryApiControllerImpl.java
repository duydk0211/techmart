package com.techmart.core.controller.inventory.impl;

import com.techmart.core.bus.SpringBus;
import com.techmart.core.bus.command.inventory.AdjustInventoryCommand;
import com.techmart.core.controller.inventory.InternalInventoryApiController;
import com.techmart.core.dto.request.inventory.InventoryAdjustRequest;
import com.techmart.core.dto.response.inventory.InventoryAdjustResponse;
import com.techmart.shared.security.TechmartPreauthorize;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/api/v1/inventory")
public class InternalInventoryApiControllerImpl implements InternalInventoryApiController {

    private static final Logger log = LoggerFactory.getLogger(InternalInventoryApiControllerImpl.class);

    private final SpringBus springBus;

    public InternalInventoryApiControllerImpl(SpringBus springBus) {
        this.springBus = springBus;
    }

    @Override
    @PostMapping("/adjust")
    @TechmartPreauthorize(gateway = "TECHMART_GW", roles = {"STAFF", "ADMIN"})
    public ResponseEntity<InventoryAdjustResponse> adjustInventory(@Valid @RequestBody InventoryAdjustRequest request) {
        log.info("[InternalInventoryApiControllerImpl][adjustInventory] Adjusting inventory for branch: {}, variant: {}",
                request.branchId(), request.variantId());
        InventoryAdjustResponse response = springBus.execute(new AdjustInventoryCommand(request));
        return ResponseEntity.ok(response);
    }
}
