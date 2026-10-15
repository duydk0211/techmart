package com.techmart.core.service.command.branch;

import com.techmart.core.dto.request.branch.BranchRequest;
import com.techmart.core.dto.response.branch.BranchResponse;

public interface BranchCommandService {

    BranchResponse createBranch(BranchRequest request);
}
