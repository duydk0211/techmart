package com.techmart.core.bus.query.branch;

import com.techmart.core.bus.Query;
import com.techmart.core.dto.response.branch.BranchResponse;

import java.util.List;

public record GetBranchListQuery() implements Query<List<BranchResponse>> {
}
