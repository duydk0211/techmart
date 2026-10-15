package com.techmart.core.handler.branch;

import com.techmart.core.bus.CommandHandler;
import com.techmart.core.bus.QueryHandler;
import com.techmart.core.bus.command.branch.CreateBranchCommand;
import com.techmart.core.bus.query.branch.GetBranchInventoryQuery;
import com.techmart.core.bus.query.branch.GetBranchListQuery;
import com.techmart.core.dto.response.branch.BranchInventoryItemResponse;
import com.techmart.core.dto.response.branch.BranchResponse;
import com.techmart.core.service.command.branch.BranchCommandService;
import com.techmart.core.service.query.branch.BranchQueryService;
import com.techmart.shared.dto.PagedResponse;
import org.springframework.stereotype.Component;

import java.util.List;

public class BranchHandler {

    @Component
    public static class CreateBranchCommandHandler implements CommandHandler<CreateBranchCommand, BranchResponse> {
        private final BranchCommandService branchCommandService;

        public CreateBranchCommandHandler(BranchCommandService branchCommandService) {
            this.branchCommandService = branchCommandService;
        }

        @Override
        public BranchResponse handle(CreateBranchCommand command) {
            return branchCommandService.createBranch(command.request());
        }
    }

    @Component
    public static class GetBranchListQueryHandler implements QueryHandler<GetBranchListQuery, List<BranchResponse>> {
        private final BranchQueryService branchQueryService;

        public GetBranchListQueryHandler(BranchQueryService branchQueryService) {
            this.branchQueryService = branchQueryService;
        }

        @Override
        public List<BranchResponse> handle(GetBranchListQuery query) {
            return branchQueryService.getBranchList();
        }
    }

    @Component
    public static class GetBranchInventoryQueryHandler implements QueryHandler<GetBranchInventoryQuery, PagedResponse<BranchInventoryItemResponse>> {
        private final BranchQueryService branchQueryService;

        public GetBranchInventoryQueryHandler(BranchQueryService branchQueryService) {
            this.branchQueryService = branchQueryService;
        }

        @Override
        public PagedResponse<BranchInventoryItemResponse> handle(GetBranchInventoryQuery query) {
            return branchQueryService.getBranchInventory(query.branchId(), query.pageable());
        }
    }
}
