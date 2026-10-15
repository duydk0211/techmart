package com.techmart.core.handler.inventory;

import com.techmart.core.bus.CommandHandler;
import com.techmart.core.bus.command.inventory.AdjustInventoryCommand;
import com.techmart.core.dto.response.inventory.InventoryAdjustResponse;
import com.techmart.core.service.command.inventory.InventoryCommandService;
import org.springframework.stereotype.Component;

public class InventoryHandler {

    @Component
    public static class AdjustInventoryCommandHandler implements CommandHandler<AdjustInventoryCommand, InventoryAdjustResponse> {
        private final InventoryCommandService inventoryCommandService;

        public AdjustInventoryCommandHandler(InventoryCommandService inventoryCommandService) {
            this.inventoryCommandService = inventoryCommandService;
        }

        @Override
        public InventoryAdjustResponse handle(AdjustInventoryCommand command) {
            return inventoryCommandService.adjustInventory(command.request());
        }
    }
}
