package com.techmart.core.bus.command.branch;

import com.techmart.core.bus.Command;
import com.techmart.core.dto.request.branch.BranchRequest;
import com.techmart.core.dto.response.branch.BranchResponse;

public record CreateBranchCommand(
        BranchRequest request
) implements Command<BranchResponse> {
}
