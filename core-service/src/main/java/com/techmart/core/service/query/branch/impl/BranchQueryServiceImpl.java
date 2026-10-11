package com.techmart.core.service.query.branch.impl;

import com.techmart.core.dto.response.branch.BranchInventoryItemResponse;
import com.techmart.core.dto.response.branch.BranchResponse;
import com.techmart.core.entity.inventory.InventoryEntity;
import com.techmart.core.repository.branch.BranchRepository;
import com.techmart.core.repository.inventory.InventoryRepository;
import com.techmart.core.service.query.branch.BranchQueryService;
import com.techmart.shared.constant.ResponseCode;
import com.techmart.shared.dto.PagedResponse;
import com.techmart.shared.exception.CoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class BranchQueryServiceImpl implements BranchQueryService {

    private static final Logger log = LoggerFactory.getLogger(BranchQueryServiceImpl.class);

    private final BranchRepository branchRepository;
    private final InventoryRepository inventoryRepository;

    public BranchQueryServiceImpl(BranchRepository branchRepository, InventoryRepository inventoryRepository) {
        this.branchRepository = branchRepository;
        this.inventoryRepository = inventoryRepository;
    }

    @Override
    public List<BranchResponse> getBranchList() {
        log.info("[BranchQueryServiceImpl][getBranchList] Fetching all active branches with masked inventory");
        return branchRepository.findAllByActiveTrueOrderByNameAsc()
                .stream()
                .map(BranchResponse::fromEntity)
                .toList();
    }

    @Override
    public PagedResponse<BranchInventoryItemResponse> getBranchInventory(UUID branchId, Pageable pageable) {
        log.info("[BranchQueryServiceImpl][getBranchInventory] Fetching internal inventory for branch: {}", branchId);

        if (!branchRepository.existsById(branchId)) {
            throw new CoreException(ResponseCode.RESOURCE_NOT_FOUND, "Branch not found: " + branchId);
        }

        Page<InventoryEntity> page = inventoryRepository.findByBranchIdWithVariantAndProduct(branchId, pageable);

        List<BranchInventoryItemResponse> items = page.getContent().stream()
                .map(inv -> new BranchInventoryItemResponse(
                        inv.getVariant().getId(),
                        inv.getVariant().getProduct().getName(),
                        inv.getVariant().getSku(),
                        inv.getVariant().getAttributes(),
                        inv.getQuantityOnHand(),
                        inv.getQuantityReserved(),
                        inv.getQuantityOnHand() - inv.getQuantityReserved(),
                        inv.getReorderLevel()
                ))
                .toList();

        return new PagedResponse<>(
                items,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }
}
