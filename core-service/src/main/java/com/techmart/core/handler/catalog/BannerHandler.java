package com.techmart.core.handler.catalog;

import com.techmart.core.bus.CommandHandler;
import com.techmart.core.bus.QueryHandler;
import com.techmart.core.bus.command.catalog.CreateBannerCommand;
import com.techmart.core.bus.command.catalog.DeleteBannerCommand;
import com.techmart.core.bus.query.catalog.GetActiveBannersQuery;
import com.techmart.core.dto.response.catalog.BannerResponse;
import com.techmart.core.service.command.catalog.BannerCommandService;
import com.techmart.core.service.query.catalog.BannerQueryService;
import org.springframework.stereotype.Component;

import java.util.List;

public class BannerHandler {

    @Component
    public static class CreateBannerCommandHandler implements CommandHandler<CreateBannerCommand, BannerResponse> {
        private final BannerCommandService bannerCommandService;

        public CreateBannerCommandHandler(BannerCommandService bannerCommandService) {
            this.bannerCommandService = bannerCommandService;
        }

        @Override
        public BannerResponse handle(CreateBannerCommand command) {
            return bannerCommandService.createBanner(command.request());
        }
    }

    @Component
    public static class DeleteBannerCommandHandler implements CommandHandler<DeleteBannerCommand, Void> {
        private final BannerCommandService bannerCommandService;

        public DeleteBannerCommandHandler(BannerCommandService bannerCommandService) {
            this.bannerCommandService = bannerCommandService;
        }

        @Override
        public Void handle(DeleteBannerCommand command) {
            bannerCommandService.deleteBanner(command.id());
            return null;
        }
    }

    @Component
    public static class GetActiveBannersQueryHandler implements QueryHandler<GetActiveBannersQuery, List<BannerResponse>> {
        private final BannerQueryService bannerQueryService;

        public GetActiveBannersQueryHandler(BannerQueryService bannerQueryService) {
            this.bannerQueryService = bannerQueryService;
        }

        @Override
        public List<BannerResponse> handle(GetActiveBannersQuery query) {
            return bannerQueryService.getActiveBanners();
        }
    }
}
