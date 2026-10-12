package com.techmart.core.controller.branch;

import com.techmart.core.dto.request.branch.BranchRequest;
import com.techmart.core.dto.response.branch.BranchInventoryItemResponse;
import com.techmart.core.dto.response.branch.BranchResponse;
import com.techmart.shared.dto.PagedResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

public interface InternalBranchApiController {

    ResponseEntity<List<BranchResponse>> getBranchList();

    ResponseEntity<BranchResponse> createBranch(BranchRequest request);

    ResponseEntity<PagedResponse<BranchInventoryItemResponse>> getBranchInventory(UUID id, Pageable pageable);
}
