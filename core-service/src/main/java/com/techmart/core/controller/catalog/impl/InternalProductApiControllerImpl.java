package com.techmart.core.controller.catalog.impl;

import com.techmart.core.bus.SpringBus;
import com.techmart.core.bus.command.catalog.AddVariantCommand;
import com.techmart.core.bus.command.catalog.CreateProductCommand;
import com.techmart.core.bus.query.catalog.GetProductBySlugQuery;
import com.techmart.core.bus.query.catalog.GetProductDetailQuery;
import com.techmart.core.bus.query.catalog.SearchProductsQuery;
import com.techmart.core.controller.catalog.InternalProductApiController;
import com.techmart.core.dto.request.catalog.ProductCreateRequest;
import com.techmart.core.dto.request.catalog.VariantCreateRequest;
import com.techmart.core.dto.response.catalog.ProductDetailResponse;
import com.techmart.core.dto.response.catalog.ProductSummaryResponse;
import com.techmart.core.dto.response.catalog.VariantDetailResponse;
import com.techmart.shared.dto.PagedResponse;
import com.techmart.shared.security.TechmartPreauthorize;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/internal/api/v1/products")
public class InternalProductApiControllerImpl implements InternalProductApiController {

    private static final Logger log = LoggerFactory.getLogger(InternalProductApiControllerImpl.class);

    private final SpringBus springBus;

    public InternalProductApiControllerImpl(SpringBus springBus) {
        this.springBus = springBus;
    }

    @Override
    @GetMapping
    @TechmartPreauthorize(gateway = "TECHMART_GW", authenticated = false)
    public ResponseEntity<PagedResponse<ProductSummaryResponse>> searchProducts(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "categoryId", required = false) UUID categoryId,
            @RequestParam(name = "brandId", required = false) UUID brandId,
            @RequestParam(name = "branchId", required = false) UUID branchId,
            @RequestParam(name = "minPrice", required = false) BigDecimal minPrice,
            @RequestParam(name = "maxPrice", required = false) BigDecimal maxPrice,
            @RequestParam(name = "inStockOnly", required = false) Boolean inStockOnly,
            @PageableDefault(page = 0, size = 20) Pageable pageable) {
        log.info("[InternalProductApiControllerImpl][searchProducts] Searching products: keyword={}, categoryId={}, brandId={}",
                keyword, categoryId, brandId);
        PagedResponse<ProductSummaryResponse> response = springBus.query(new SearchProductsQuery(
                keyword, categoryId, brandId, branchId, minPrice, maxPrice, inStockOnly, pageable
        ));
        return ResponseEntity.ok(response);
    }

    @Override
    @GetMapping("/{id}")
    @TechmartPreauthorize(gateway = "TECHMART_GW", authenticated = false)
    public ResponseEntity<ProductDetailResponse> getProductDetail(@PathVariable("id") UUID id) {
        log.info("[InternalProductApiControllerImpl][getProductDetail] Fetching product detail by id: {}", id);
        ProductDetailResponse response = springBus.query(new GetProductDetailQuery(id));
        return ResponseEntity.ok(response);
    }

    @Override
    @GetMapping("/slug/{slug}")
    @TechmartPreauthorize(gateway = "TECHMART_GW", authenticated = false)
    public ResponseEntity<ProductDetailResponse> getProductBySlug(@PathVariable("slug") String slug) {
        log.info("[InternalProductApiControllerImpl][getProductBySlug] Fetching product detail by slug: {}", slug);
        ProductDetailResponse response = springBus.query(new GetProductBySlugQuery(slug));
        return ResponseEntity.ok(response);
    }

    @Override
    @PostMapping
    @TechmartPreauthorize(gateway = "TECHMART_GW", roles = {"STAFF", "ADMIN"})
    public ResponseEntity<ProductDetailResponse> createProduct(@Valid @RequestBody ProductCreateRequest request) {
        log.info("[InternalProductApiControllerImpl][createProduct] Creating product: {}", request.name());
        ProductDetailResponse response = springBus.execute(new CreateProductCommand(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Override
    @PostMapping("/{id}/variants")
    @TechmartPreauthorize(gateway = "TECHMART_GW", roles = {"STAFF", "ADMIN"})
    public ResponseEntity<VariantDetailResponse> addVariant(@PathVariable("id") UUID id, @Valid @RequestBody VariantCreateRequest request) {
        log.info("[InternalProductApiControllerImpl][addVariant] Adding variant to product: {}", id);
        VariantDetailResponse response = springBus.execute(new AddVariantCommand(id, request));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
