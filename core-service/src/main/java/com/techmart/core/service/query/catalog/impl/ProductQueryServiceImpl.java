package com.techmart.core.service.query.catalog.impl;

import com.techmart.core.bus.query.catalog.SearchProductsQuery;
import com.techmart.core.dto.response.catalog.BrandSummaryDto;
import com.techmart.core.dto.response.catalog.ProductDetailResponse;
import com.techmart.core.dto.response.catalog.ProductImageResponse;
import com.techmart.core.dto.response.catalog.ProductSummaryProjectionRow;
import com.techmart.core.dto.response.catalog.ProductSummaryResponse;
import com.techmart.core.dto.response.catalog.StockDistributionDto;
import com.techmart.core.dto.response.catalog.VariantDetailResponse;
import com.techmart.core.entity.catalog.CategoryEntity;
import com.techmart.core.entity.catalog.ProductEntity;
import com.techmart.core.entity.catalog.ProductImageEntity;
import com.techmart.core.entity.catalog.ProductVariantEntity;
import com.techmart.core.entity.inventory.InventoryEntity;
import com.techmart.core.repository.catalog.CategoryRepository;
import com.techmart.core.repository.catalog.ProductImageRepository;
import com.techmart.core.repository.catalog.ProductRepository;
import com.techmart.core.repository.catalog.ProductVariantRepository;
import com.techmart.core.repository.inventory.InventoryRepository;
import com.techmart.core.service.query.catalog.ProductQueryService;
import com.techmart.shared.constant.ResponseCode;
import com.techmart.shared.dto.PagedResponse;
import com.techmart.shared.exception.CoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ProductQueryServiceImpl implements ProductQueryService {

    private static final Logger log = LoggerFactory.getLogger(ProductQueryServiceImpl.class);

    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductImageRepository productImageRepository;
    private final CategoryRepository categoryRepository;
    private final InventoryRepository inventoryRepository;

    public ProductQueryServiceImpl(
            ProductRepository productRepository,
            ProductVariantRepository productVariantRepository,
            ProductImageRepository productImageRepository,
            CategoryRepository categoryRepository,
            InventoryRepository inventoryRepository) {
        this.productRepository = productRepository;
        this.productVariantRepository = productVariantRepository;
        this.productImageRepository = productImageRepository;
        this.categoryRepository = categoryRepository;
        this.inventoryRepository = inventoryRepository;
    }

    @Override
    public PagedResponse<ProductSummaryResponse> searchProducts(SearchProductsQuery query) {
        log.info("[ProductQueryServiceImpl][searchProducts] Searching products with keyword: {}, categoryId: {}, brandId: {}",
                query.keyword(), query.categoryId(), query.brandId());

        String categoryPath = null;
        if (query.categoryId() != null) {
            CategoryEntity cat = categoryRepository.findById(query.categoryId()).orElse(null);
            if (cat != null) {
                categoryPath = cat.getPath();
            }
        }

        boolean inStockOnly = query.inStockOnly() != null && query.inStockOnly();

        Page<ProductSummaryProjectionRow> page = productRepository.searchProducts(
                query.keyword(),
                categoryPath,
                query.brandId(),
                query.branchId(),
                query.minPrice(),
                query.maxPrice(),
                inStockOnly,
                query.pageable()
        );

        List<ProductSummaryResponse> items = page.getContent().stream()
                .map(ProductSummaryResponse::fromProjectionRow)
                .toList();

        return new PagedResponse<>(
                items,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }

    @Override
    public ProductDetailResponse getProductDetail(UUID id) {
        log.info("[ProductQueryServiceImpl][getProductDetail] Fetching product detail by id: {}", id);
        ProductEntity product = productRepository.findById(id)
                .orElseThrow(() -> new CoreException(ResponseCode.RESOURCE_NOT_FOUND, "Product not found: " + id));

        return buildProductDetailResponse(product);
    }

    @Override
    public ProductDetailResponse getProductBySlug(String slug) {
        log.info("[ProductQueryServiceImpl][getProductBySlug] Fetching product detail by slug: {}", slug);
        ProductEntity product = productRepository.findBySlug(slug)
                .orElseThrow(() -> new CoreException(ResponseCode.RESOURCE_NOT_FOUND, "Product not found with slug: " + slug));

        return buildProductDetailResponse(product);
    }

    private ProductDetailResponse buildProductDetailResponse(ProductEntity product) {
        List<ProductImageEntity> images = productImageRepository.findByProductIdOrderByDisplayOrderAsc(product.getId());
        List<ProductImageResponse> imageResponses = images.stream()
                .map(ProductImageResponse::fromEntity)
                .toList();

        List<ProductVariantEntity> variants = productVariantRepository.findByProductIdAndStatus(product.getId(), "ACTIVE");
        List<VariantDetailResponse> variantResponses = new ArrayList<>();

        for (ProductVariantEntity variant : variants) {
            BigDecimal discountMultiplier = BigDecimal.ONE.subtract(
                    variant.getDiscountPercent().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)
            );
            BigDecimal effectivePrice = variant.getPrice().multiply(discountMultiplier).setScale(2, RoundingMode.HALF_UP);

            List<InventoryEntity> inventories = inventoryRepository.findByVariantId(variant.getId());
            List<StockDistributionDto> distribution = inventories.stream()
                    .map(i -> new StockDistributionDto(
                            i.getBranch().getId(),
                            i.getBranch().getName(),
                            i.getBranch().getAddress(),
                            i.getQuantityOnHand() - i.getQuantityReserved()
                    ))
                    .toList();

            int totalStock = distribution.stream().mapToInt(StockDistributionDto::availableQuantity).sum();

            variantResponses.add(new VariantDetailResponse(
                    variant.getId(),
                    variant.getSku(),
                    variant.getAttributes(),
                    variant.getPrice(),
                    variant.getDiscountPercent(),
                    effectivePrice,
                    variant.getImageUrl(),
                    variant.getStatus(),
                    totalStock,
                    distribution
            ));
        }

        String breadcrumb = buildBreadcrumb(product.getCategory());

        ProductDetailResponse.CategoryDetailDto catDto = new ProductDetailResponse.CategoryDetailDto(
                product.getCategory().getId(),
                product.getCategory().getName(),
                breadcrumb
        );

        BrandSummaryDto brandDto = new BrandSummaryDto(
                product.getBrand().getId(),
                product.getBrand().getName(),
                product.getBrand().getLogoUrl()
        );

        return new ProductDetailResponse(
                product.getId(),
                product.getName(),
                product.getSlug(),
                product.getDescription(),
                product.getStatus(),
                catDto,
                brandDto,
                imageResponses,
                variantResponses
        );
    }

    private String buildBreadcrumb(CategoryEntity category) {
        if (category == null || category.getPath() == null) {
            return "";
        }
        String[] parts = category.getPath().split("/");
        return Arrays.stream(parts)
                .filter(p -> !p.isBlank())
                .collect(Collectors.joining(" > "));
    }
}
