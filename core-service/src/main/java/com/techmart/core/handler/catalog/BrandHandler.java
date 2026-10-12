package com.techmart.core.handler.catalog;

import com.techmart.core.bus.CommandHandler;
import com.techmart.core.bus.QueryHandler;
import com.techmart.core.bus.command.catalog.CreateBrandCommand;
import com.techmart.core.bus.query.catalog.GetBrandListQuery;
import com.techmart.core.dto.response.catalog.BrandResponse;
import com.techmart.core.service.command.catalog.BrandCommandService;
import com.techmart.core.service.query.catalog.BrandQueryService;
import org.springframework.stereotype.Component;

import java.util.List;

public class BrandHandler {

    @Component
    public static class CreateBrandCommandHandler implements CommandHandler<CreateBrandCommand, BrandResponse> {
        private final BrandCommandService brandCommandService;

        public CreateBrandCommandHandler(BrandCommandService brandCommandService) {
            this.brandCommandService = brandCommandService;
        }

        @Override
        public BrandResponse handle(CreateBrandCommand command) {
            return brandCommandService.createBrand(command.request());
        }
    }

    @Component
    public static class GetBrandListQueryHandler implements QueryHandler<GetBrandListQuery, List<BrandResponse>> {
        private final BrandQueryService brandQueryService;

        public GetBrandListQueryHandler(BrandQueryService brandQueryService) {
            this.brandQueryService = brandQueryService;
        }

        @Override
        public List<BrandResponse> handle(GetBrandListQuery query) {
            return brandQueryService.getBrandList();
        }
    }
}
