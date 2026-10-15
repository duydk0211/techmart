package com.techmart.core.service.command.inventory.impl;

import com.techmart.core.dto.request.inventory.InventoryAdjustRequest;
import com.techmart.core.dto.response.inventory.InventoryAdjustResponse;
import com.techmart.core.entity.branch.BranchEntity;
import com.techmart.core.entity.catalog.ProductVariantEntity;
import com.techmart.core.entity.inventory.InventoryEntity;
import com.techmart.core.entity.inventory.InventoryHistoryEntity;
import com.techmart.core.repository.branch.BranchRepository;
import com.techmart.core.repository.catalog.ProductVariantRepository;
import com.techmart.core.repository.inventory.InventoryHistoryRepository;
import com.techmart.core.repository.inventory.InventoryRepository;
import com.techmart.core.service.command.inventory.InventoryCommandService;
import com.techmart.shared.constant.ResponseCode;
import com.techmart.shared.exception.CoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@Transactional
public class InventoryCommandServiceImpl implements InventoryCommandService {

    private static final Logger log = LoggerFactory.getLogger(InventoryCommandServiceImpl.class);

    private final BranchRepository branchRepository;
    private final ProductVariantRepository productVariantRepository;
    private final InventoryRepository inventoryRepository;
    private final InventoryHistoryRepository inventoryHistoryRepository;

    public InventoryCommandServiceImpl(
            BranchRepository branchRepository,
            ProductVariantRepository productVariantRepository,
            InventoryRepository inventoryRepository,
            InventoryHistoryRepository inventoryHistoryRepository) {
        this.branchRepository = branchRepository;
        this.productVariantRepository = productVariantRepository;
        this.inventoryRepository = inventoryRepository;
        this.inventoryHistoryRepository = inventoryHistoryRepository;
    }

    @Override
    public InventoryAdjustResponse adjustInventory(InventoryAdjustRequest request) {
        log.info("[InventoryCommandServiceImpl][adjustInventory] Adjusting inventory for branch: {}, variant: {}, type: {}, change: {}",
                request.branchId(), request.variantId(), request.changeType(), request.quantityChange());

        BranchEntity branch = branchRepository.findById(request.branchId())
                .orElseThrow(() -> new CoreException(ResponseCode.RESOURCE_NOT_FOUND, "Branch not found: " + request.branchId()));

        ProductVariantEntity variant = productVariantRepository.findById(request.variantId())
                .orElseThrow(() -> new CoreException(ResponseCode.RESOURCE_NOT_FOUND, "Product variant not found: " + request.variantId()));

        InventoryEntity inventory = inventoryRepository.findByBranchIdAndVariantId(request.branchId(), request.variantId())
                .orElseGet(() -> {
                    if ("STOCK_IN".equalsIgnoreCase(request.changeType())) {
                        InventoryEntity newInv = new InventoryEntity();
                        newInv.setBranch(branch);
                        newInv.setVariant(variant);
                        newInv.setQuantityOnHand(0);
                        newInv.setQuantityReserved(0);
                        newInv.setReorderLevel(5);
                        newInv.setVersion(0L);
                        return newInv;
                    }
                    throw new CoreException(ResponseCode.INSUFFICIENT_STOCK, "Inventory item does not exist for reduction or adjustment");
                });

        int delta;
        if ("STOCK_IN".equalsIgnoreCase(request.changeType())) {
            delta = Math.abs(request.quantityChange());
        } else if ("STOCK_OUT".equalsIgnoreCase(request.changeType())) {
            delta = -Math.abs(request.quantityChange());
        } else {
            delta = request.quantityChange();
        }

        int currentOnHand = inventory.getQuantityOnHand();
        int currentReserved = inventory.getQuantityReserved();
        int newOnHand = currentOnHand + delta;

        if (newOnHand < currentReserved || newOnHand < 0) {
            throw new CoreException(ResponseCode.INSUFFICIENT_STOCK,
                    "New on-hand quantity cannot be less than reserved quantity or negative");
        }

        inventory.setQuantityOnHand(newOnHand);
        inventoryRepository.save(inventory);

        branch.setQuantityAvailable(branch.getQuantityAvailable() + delta);
        branchRepository.save(branch);

        InventoryHistoryEntity history = new InventoryHistoryEntity();
        history.setBranch(branch);
        history.setVariant(variant);
        history.setChangeType(request.changeType());
        history.setQuantityChange(delta);
        history.setQuantityBefore(currentOnHand);
        history.setQuantityAfter(newOnHand);
        history.setNote(request.note());
        inventoryHistoryRepository.save(history);

        log.info("[InventoryCommandServiceImpl][adjustInventory] Completed adjustment. OldOnHand: {}, NewOnHand: {}, Reserved: {}",
                currentOnHand, newOnHand, currentReserved);

        return new InventoryAdjustResponse(
                branch.getId(),
                variant.getId(),
                currentOnHand,
                newOnHand,
                currentReserved,
                newOnHand - currentReserved,
                Instant.now()
        );
    }
}
