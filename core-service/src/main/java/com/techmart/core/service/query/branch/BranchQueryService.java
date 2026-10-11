package com.techmart.core.service.query.branch;

import com.techmart.core.dto.response.branch.BranchInventoryItemResponse;
import com.techmart.core.dto.response.branch.BranchResponse;
import com.techmart.shared.dto.PagedResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface BranchQueryService {

    List<BranchResponse> getBranchList();

    PagedResponse<BranchInventoryItemResponse> getBranchInventory(UUID branchId, Pageable pageable);
}
