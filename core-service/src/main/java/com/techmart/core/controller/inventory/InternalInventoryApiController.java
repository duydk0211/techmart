package com.techmart.core.controller.inventory;

import com.techmart.core.dto.request.inventory.InventoryAdjustRequest;
import com.techmart.core.dto.response.inventory.InventoryAdjustResponse;
import org.springframework.http.ResponseEntity;

public interface InternalInventoryApiController {

    ResponseEntity<InventoryAdjustResponse> adjustInventory(InventoryAdjustRequest request);
}
