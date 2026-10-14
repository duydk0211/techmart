package com.techmart.core.bus.command.catalog;

import com.techmart.core.bus.Command;
import com.techmart.core.dto.request.catalog.BannerRequest;
import com.techmart.core.dto.response.catalog.BannerResponse;

public record CreateBannerCommand(BannerRequest request) implements Command<BannerResponse> {
}
