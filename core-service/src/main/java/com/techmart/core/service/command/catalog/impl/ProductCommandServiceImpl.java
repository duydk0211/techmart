package com.techmart.core.service.command.catalog.impl;

import com.techmart.core.dto.request.catalog.InitialStockRequest;
import com.techmart.core.dto.request.catalog.ProductCreateRequest;
import com.techmart.core.dto.request.catalog.ProductImageRequest;
import com.techmart.core.dto.request.catalog.VariantCreateRequest;
import com.techmart.core.dto.response.catalog.BrandSummaryDto;
import com.techmart.core.dto.response.catalog.ProductDetailResponse;
import com.techmart.core.dto.response.catalog.ProductImageResponse;
import com.techmart.core.dto.response.catalog.StockDistributionDto;
import com.techmart.core.dto.response.catalog.VariantDetailResponse;
import com.techmart.core.entity.branch.BranchEntity;
import com.techmart.core.entity.catalog.BrandEntity;
import com.techmart.core.entity.catalog.CategoryEntity;
import com.techmart.core.entity.catalog.ProductEntity;
import com.techmart.core.entity.catalog.ProductImageEntity;
import com.techmart.core.entity.catalog.ProductVariantEntity;
import com.techmart.core.entity.inventory.InventoryEntity;
import com.techmart.core.entity.inventory.InventoryHistoryEntity;
import com.techmart.core.repository.branch.BranchRepository;
import com.techmart.core.repository.catalog.BrandRepository;
import com.techmart.core.repository.catalog.CategoryRepository;
import com.techmart.core.repository.catalog.ProductImageRepository;
import com.techmart.core.repository.catalog.ProductRepository;
import com.techmart.core.repository.catalog.ProductVariantRepository;
import com.techmart.core.repository.inventory.InventoryHistoryRepository;
import com.techmart.core.repository.inventory.InventoryRepository;
import com.techmart.core.service.command.catalog.ProductCommandService;
import com.techmart.core.util.SlugUtils;
import com.techmart.shared.constant.ResponseCode;
import com.techmart.shared.exception.CoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class ProductCommandServiceImpl implements ProductCommandService {

    private static final Logger log = LoggerFactory.getLogger(ProductCommandServiceImpl.class);

    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductImageRepository productImageRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final BranchRepository branchRepository;
    private final InventoryRepository inventoryRepository;
    private final InventoryHistoryRepository inventoryHistoryRepository;

    public ProductCommandServiceImpl(
            ProductRepository productRepository,
            ProductVariantRepository productVariantRepository,
            ProductImageRepository productImageRepository,
            CategoryRepository categoryRepository,
            BrandRepository brandRepository,
            BranchRepository branchRepository,
            InventoryRepository inventoryRepository,
            InventoryHistoryRepository inventoryHistoryRepository) {
        this.productRepository = productRepository;
        this.productVariantRepository = productVariantRepository;
        this.productImageRepository = productImageRepository;
        this.categoryRepository = categoryRepository;
        this.brandRepository = brandRepository;
        this.branchRepository = branchRepository;
        this.inventoryRepository = inventoryRepository;
        this.inventoryHistoryRepository = inventoryHistoryRepository;
    }

    @Override
    public ProductDetailResponse createProduct(ProductCreateRequest request) {
        log.info("[ProductCommandServiceImpl][createProduct] Creating product: {}", request.name());

        CategoryEntity category = categoryRepository.findById(request.categoryId())
                .filter(CategoryEntity::isActive)
                .orElseThrow(() -> new CoreException(ResponseCode.RESOURCE_NOT_FOUND, "Category not found or inactive: " + request.categoryId()));

        BrandEntity brand = brandRepository.findById(request.brandId())
                .filter(BrandEntity::isActive)
                .orElseThrow(() -> new CoreException(ResponseCode.RESOURCE_NOT_FOUND, "Brand not found or inactive: " + request.brandId()));

        String slug = (request.slug() != null && !request.slug().isBlank())
                ? SlugUtils.toSlug(request.slug())
                : SlugUtils.toSlug(request.name());

        if (productRepository.existsBySlug(slug)) {
            throw new CoreException(ResponseCode.DUPLICATE_RESOURCE, "Product slug already exists: " + slug);
        }

        ProductEntity product = new ProductEntity();
        product.setCategory(category);
        product.setBrand(brand);
        product.setName(request.name());
        product.setSlug(slug);
        product.setDescription(request.description());
        product.setThumbnailUrl(request.thumbnailUrl());
        product.setStatus("ACTIVE");

        ProductEntity saved = productRepository.save(product);

        List<ProductImageResponse> imageResponses = new ArrayList<>();
        if (request.images() != null && !request.images().isEmpty()) {
            boolean primaryFound = false;
            for (ProductImageRequest imgReq : request.images()) {
                if (imgReq.primaryOrDefault()) {
                    primaryFound = true;
                    break;
                }
            }

            boolean isFirst = true;
            for (ProductImageRequest imgReq : request.images()) {
                ProductImageEntity img = new ProductImageEntity();
                img.setProduct(saved);
                img.setImageUrl(imgReq.imageUrl());
                img.setDisplayOrder(imgReq.displayOrderOrDefault());

                if (!primaryFound && isFirst) {
                    img.setPrimary(true);
                } else if (imgReq.primaryOrDefault()) {
                    img.setPrimary(true);
                    primaryFound = false; // ensure only one is primary
                } else {
                    img.setPrimary(false);
                }
                isFirst = false;

                ProductImageEntity savedImg = productImageRepository.save(img);
                imageResponses.add(ProductImageResponse.fromEntity(savedImg));
            }
        }

        log.info("[ProductCommandServiceImpl][createProduct] Created product id: {}, slug: {}", saved.getId(), saved.getSlug());

        ProductDetailResponse.CategoryDetailDto catDto = new ProductDetailResponse.CategoryDetailDto(
                category.getId(),
                category.getName(),
                category.getPath()
        );

        BrandSummaryDto brandDto = new BrandSummaryDto(
                brand.getId(),
                brand.getName(),
                brand.getLogoUrl()
        );

        return new ProductDetailResponse(
                saved.getId(),
                saved.getName(),
                saved.getSlug(),
                saved.getDescription(),
                saved.getStatus(),
                catDto,
                brandDto,
                imageResponses,
                new ArrayList<>()
        );
    }

    @Override
    public VariantDetailResponse addVariant(UUID productId, VariantCreateRequest request) {
        log.info("[ProductCommandServiceImpl][addVariant] Adding variant SKU: {} to product: {}", request.sku(), productId);

        ProductEntity product = productRepository.findById(productId)
                .orElseThrow(() -> new CoreException(ResponseCode.RESOURCE_NOT_FOUND, "Product not found: " + productId));

        if (productVariantRepository.existsBySku(request.sku())) {
            throw new CoreException(ResponseCode.DUPLICATE_RESOURCE, "Variant with SKU already exists: " + request.sku());
        }

        ProductVariantEntity variant = new ProductVariantEntity();
        variant.setProduct(product);
        variant.setSku(request.sku());
        variant.setAttributes(request.attributes() != null ? request.attributes() : Map.of());
        variant.setPrice(request.price());
        variant.setDiscountPercent(request.discountPercentOrDefault());
        variant.setImageUrl(request.imageUrl());
        variant.setStatus("ACTIVE");

        ProductVariantEntity savedVariant = productVariantRepository.save(variant);

        if (request.initialStockByBranch() != null) {
            for (InitialStockRequest stockReq : request.initialStockByBranch()) {
                BranchEntity branch = branchRepository.findById(stockReq.branchId())
                        .orElseThrow(() -> new CoreException(ResponseCode.RESOURCE_NOT_FOUND, "Branch not found: " + stockReq.branchId()));

                InventoryEntity inv = new InventoryEntity();
                inv.setBranch(branch);
                inv.setVariant(savedVariant);
                inv.setQuantityOnHand(stockReq.quantityOnHand());
                inv.setQuantityReserved(0);
                inv.setReorderLevel(stockReq.reorderLevelOrDefault());
                inv.setVersion(0L);
                inventoryRepository.save(inv);

                branch.setQuantityAvailable(branch.getQuantityAvailable() + stockReq.quantityOnHand());
                branchRepository.save(branch);

                InventoryHistoryEntity history = new InventoryHistoryEntity();
                history.setBranch(branch);
                history.setVariant(savedVariant);
                history.setChangeType("STOCK_IN");
                history.setQuantityChange(stockReq.quantityOnHand());
                history.setQuantityBefore(0);
                history.setQuantityAfter(stockReq.quantityOnHand());
                history.setNote("Initial stock upon variant creation");
                inventoryHistoryRepository.save(history);
            }
        }

        BigDecimal discountMultiplier = BigDecimal.ONE.subtract(
                savedVariant.getDiscountPercent().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)
        );
        BigDecimal effectivePrice = savedVariant.getPrice().multiply(discountMultiplier).setScale(2, RoundingMode.HALF_UP);

        List<InventoryEntity> inventories = inventoryRepository.findByVariantId(savedVariant.getId());
        List<StockDistributionDto> distribution = inventories.stream()
                .map(i -> new StockDistributionDto(
                        i.getBranch().getId(),
                        i.getBranch().getName(),
                        i.getBranch().getAddress(),
                        i.getQuantityOnHand() - i.getQuantityReserved()
                ))
                .toList();

        int totalStock = distribution.stream().mapToInt(StockDistributionDto::availableQuantity).sum();

        log.info("[ProductCommandServiceImpl][addVariant] Created variant id: {}, SKU: {}", savedVariant.getId(), savedVariant.getSku());

        return new VariantDetailResponse(
                savedVariant.getId(),
                savedVariant.getSku(),
                savedVariant.getAttributes(),
                savedVariant.getPrice(),
                savedVariant.getDiscountPercent(),
                effectivePrice,
                savedVariant.getImageUrl(),
                savedVariant.getStatus(),
                totalStock,
                distribution
        );
    }
}
