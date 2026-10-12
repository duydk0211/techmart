package com.techmart.core.service.command.inventory;

import com.techmart.core.dto.request.inventory.InventoryAdjustRequest;
import com.techmart.core.dto.response.inventory.InventoryAdjustResponse;

public interface InventoryCommandService {

    InventoryAdjustResponse adjustInventory(InventoryAdjustRequest request);
}
