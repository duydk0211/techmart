package com.techmart.core.bus.command.inventory;

import com.techmart.core.bus.Command;
import com.techmart.core.dto.request.inventory.InventoryAdjustRequest;
import com.techmart.core.dto.response.inventory.InventoryAdjustResponse;

public record AdjustInventoryCommand(
        InventoryAdjustRequest request
) implements Command<InventoryAdjustResponse> {
}
