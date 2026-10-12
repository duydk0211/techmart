package com.techmart.core.bus.query.branch;

import com.techmart.core.bus.Query;
import com.techmart.core.dto.response.branch.BranchInventoryItemResponse;
import com.techmart.shared.dto.PagedResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public record GetBranchInventoryQuery(
        UUID branchId,
        Pageable pageable
) implements Query<PagedResponse<BranchInventoryItemResponse>> {
}
