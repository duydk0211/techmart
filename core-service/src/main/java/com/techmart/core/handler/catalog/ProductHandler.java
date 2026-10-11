package com.techmart.core.handler.catalog;

import com.techmart.core.bus.CommandHandler;
import com.techmart.core.bus.QueryHandler;
import com.techmart.core.bus.command.catalog.AddVariantCommand;
import com.techmart.core.bus.command.catalog.CreateProductCommand;
import com.techmart.core.bus.query.catalog.GetProductBySlugQuery;
import com.techmart.core.bus.query.catalog.GetProductDetailQuery;
import com.techmart.core.bus.query.catalog.SearchProductsQuery;
import com.techmart.core.dto.response.catalog.ProductDetailResponse;
import com.techmart.core.dto.response.catalog.ProductSummaryResponse;
import com.techmart.core.dto.response.catalog.VariantDetailResponse;
import com.techmart.core.service.command.catalog.ProductCommandService;
import com.techmart.core.service.query.catalog.ProductQueryService;
import com.techmart.shared.dto.PagedResponse;
import org.springframework.stereotype.Component;

public class ProductHandler {

    @Component
    public static class CreateProductCommandHandler implements CommandHandler<CreateProductCommand, ProductDetailResponse> {
        private final ProductCommandService productCommandService;

        public CreateProductCommandHandler(ProductCommandService productCommandService) {
            this.productCommandService = productCommandService;
        }

        @Override
        public ProductDetailResponse handle(CreateProductCommand command) {
            return productCommandService.createProduct(command.request());
        }
    }

    @Component
    public static class AddVariantCommandHandler implements CommandHandler<AddVariantCommand, VariantDetailResponse> {
        private final ProductCommandService productCommandService;

        public AddVariantCommandHandler(ProductCommandService productCommandService) {
            this.productCommandService = productCommandService;
        }

        @Override
        public VariantDetailResponse handle(AddVariantCommand command) {
            return productCommandService.addVariant(command.productId(), command.request());
        }
    }

    @Component
    public static class SearchProductsQueryHandler implements QueryHandler<SearchProductsQuery, PagedResponse<ProductSummaryResponse>> {
        private final ProductQueryService productQueryService;

        public SearchProductsQueryHandler(ProductQueryService productQueryService) {
            this.productQueryService = productQueryService;
        }

        @Override
        public PagedResponse<ProductSummaryResponse> handle(SearchProductsQuery query) {
            return productQueryService.searchProducts(query);
        }
    }

    @Component
    public static class GetProductDetailQueryHandler implements QueryHandler<GetProductDetailQuery, ProductDetailResponse> {
        private final ProductQueryService productQueryService;

        public GetProductDetailQueryHandler(ProductQueryService productQueryService) {
            this.productQueryService = productQueryService;
        }

        @Override
        public ProductDetailResponse handle(GetProductDetailQuery query) {
            return productQueryService.getProductDetail(query.id());
        }
    }

    @Component
    public static class GetProductBySlugQueryHandler implements QueryHandler<GetProductBySlugQuery, ProductDetailResponse> {
        private final ProductQueryService productQueryService;

        public GetProductBySlugQueryHandler(ProductQueryService productQueryService) {
            this.productQueryService = productQueryService;
        }

        @Override
        public ProductDetailResponse handle(GetProductBySlugQuery query) {
            return productQueryService.getProductBySlug(query.slug());
        }
    }
}
