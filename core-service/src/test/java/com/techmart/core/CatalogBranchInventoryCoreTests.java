package com.techmart.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.techmart.core.bus.query.catalog.SearchProductsQuery;
import com.techmart.core.dto.request.branch.BranchRequest;
import com.techmart.core.dto.request.catalog.BrandRequest;
import com.techmart.core.dto.request.catalog.CategoryRequest;
import com.techmart.core.dto.request.catalog.InitialStockRequest;
import com.techmart.core.dto.request.catalog.ProductCreateRequest;
import com.techmart.core.dto.request.catalog.ProductImageRequest;
import com.techmart.core.dto.request.catalog.VariantCreateRequest;
import com.techmart.core.dto.request.inventory.InventoryAdjustRequest;
import com.techmart.core.dto.response.branch.BranchInventoryItemResponse;
import com.techmart.core.dto.response.branch.BranchResponse;
import com.techmart.core.dto.response.catalog.BrandResponse;
import com.techmart.core.dto.response.catalog.CategoryResponse;
import com.techmart.core.dto.response.catalog.CategoryTreeResponse;
import com.techmart.core.dto.response.catalog.ProductDetailResponse;
import com.techmart.core.dto.response.catalog.ProductSummaryResponse;
import com.techmart.core.dto.response.catalog.VariantDetailResponse;
import com.techmart.core.dto.response.inventory.InventoryAdjustResponse;
import com.techmart.core.entity.branch.BranchEntity;
import com.techmart.core.entity.catalog.CategoryEntity;
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
import com.techmart.core.service.command.branch.BranchCommandService;
import com.techmart.core.service.command.catalog.BrandCommandService;
import com.techmart.core.service.command.catalog.CategoryCommandService;
import com.techmart.core.service.command.catalog.ProductCommandService;
import com.techmart.core.service.command.inventory.InventoryCommandService;
import com.techmart.core.service.query.branch.BranchQueryService;
import com.techmart.core.service.query.catalog.BrandQueryService;
import com.techmart.core.service.query.catalog.CategoryQueryService;
import com.techmart.core.service.query.catalog.ProductQueryService;
import com.techmart.shared.constant.HeaderConstants;
import com.techmart.shared.constant.ResponseCode;
import com.techmart.shared.dto.PagedResponse;
import com.techmart.shared.exception.CoreException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CatalogBranchInventoryCoreTests {

    private static final String VALID_INTERNAL_SECRET = "techmart_internal_secret_2026";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CategoryCommandService categoryCommandService;

    @Autowired
    private CategoryQueryService categoryQueryService;

    @Autowired
    private BrandCommandService brandCommandService;

    @Autowired
    private BrandQueryService brandQueryService;

    @Autowired
    private BranchCommandService branchCommandService;

    @Autowired
    private BranchQueryService branchQueryService;

    @Autowired
    private ProductCommandService productCommandService;

    @Autowired
    private ProductQueryService productQueryService;

    @Autowired
    private InventoryCommandService inventoryCommandService;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductVariantRepository productVariantRepository;

    @Autowired
    private ProductImageRepository productImageRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private InventoryHistoryRepository inventoryHistoryRepository;

    @BeforeEach
    void setUp() {
        inventoryHistoryRepository.deleteAll();
        inventoryRepository.deleteAll();
        productImageRepository.deleteAll();
        productVariantRepository.deleteAll();
        productRepository.deleteAll();
        branchRepository.deleteAll();
        brandRepository.deleteAll();

        // Safely clean up categories bottom-up to avoid self-referencing foreign key violations
        categoryRepository.findAll().stream()
                .sorted((a, b) -> Integer.compare(b.getLevel(), a.getLevel()))
                .forEach(c -> categoryRepository.deleteById(c.getId()));
    }

    @Test
    @DisplayName("TC-02: Category creation - Should create root and nested child categories with correct level and materialized path")
    void testCreateRootAndChildCategories() {
        CategoryRequest rootReq = new CategoryRequest("Thiết Bị Điện Tử", null, "Root category");
        CategoryResponse root = categoryCommandService.createCategory(rootReq);

        assertNotNull(root.id());
        assertEquals("Thiết Bị Điện Tử", root.name());
        assertEquals(1, root.level());
        assertEquals("/thiet-bi-dien-tu", root.path());

        CategoryRequest child2Req = new CategoryRequest("Điện Thoại Di Động", root.id(), "Level 2 category");
        CategoryResponse child2 = categoryCommandService.createCategory(child2Req);

        assertEquals(2, child2.level());
        assertEquals("/thiet-bi-dien-tu/dien-thoai-di-dong", child2.path());
        assertEquals(root.id(), child2.parentId());

        CategoryRequest child3Req = new CategoryRequest("Điện Thoại Thông Minh", child2.id(), "Level 3 category");
        CategoryResponse child3 = categoryCommandService.createCategory(child3Req);

        assertEquals(3, child3.level());
        assertEquals("/thiet-bi-dien-tu/dien-thoai-di-dong/dien-thoai-thong-minh", child3.path());

        CategoryRequest child4Req = new CategoryRequest("iPhone", child3.id(), "Level 4 category");
        CategoryResponse child4 = categoryCommandService.createCategory(child4Req);

        assertEquals(4, child4.level());
        assertEquals("/thiet-bi-dien-tu/dien-thoai-di-dong/dien-thoai-thong-minh/iphone", child4.path());
    }

    @Test
    @DisplayName("TC-03: Category depth constraint - Should reject creation of level 5 category with CATEGORY_MAX_LEVEL_EXCEEDED")
    void testCategoryMaxDepthExceeded() {
        CategoryResponse c1 = categoryCommandService.createCategory(new CategoryRequest("Level 1", null, null));
        CategoryResponse c2 = categoryCommandService.createCategory(new CategoryRequest("Level 2", c1.id(), null));
        CategoryResponse c3 = categoryCommandService.createCategory(new CategoryRequest("Level 3", c2.id(), null));
        CategoryResponse c4 = categoryCommandService.createCategory(new CategoryRequest("Level 4", c3.id(), null));

        CategoryRequest c5Req = new CategoryRequest("Level 5 Attempt", c4.id(), "Exceeds max level");
        CoreException ex = assertThrows(CoreException.class, () -> categoryCommandService.createCategory(c5Req));
        assertEquals(ResponseCode.CATEGORY_MAX_LEVEL_EXCEEDED, ex.getResponseCode());
    }

    @Test
    @DisplayName("TC-04: Category cycle detection - Should reject self-reference cycle with CATEGORY_CYCLE_DETECTED")
    void testCategoryCycleDetectionSelfReference() {
        CategoryResponse c1 = categoryCommandService.createCategory(new CategoryRequest("Root Node", null, null));

        CategoryRequest cycleReq = new CategoryRequest("Root Node Updated", c1.id(), "Cycle to self");
        CoreException ex = assertThrows(CoreException.class, () -> categoryCommandService.updateCategory(c1.id(), cycleReq));
        assertEquals(ResponseCode.CATEGORY_CYCLE_DETECTED, ex.getResponseCode());
    }

    @Test
    @DisplayName("TC-04: Category cycle detection - Should reject descendant cycle with CATEGORY_CYCLE_DETECTED")
    void testCategoryCycleDetectionDescendant() {
        CategoryResponse parent = categoryCommandService.createCategory(new CategoryRequest("Parent Cat", null, null));
        CategoryResponse child = categoryCommandService.createCategory(new CategoryRequest("Child Cat", parent.id(), null));
        CategoryResponse grandchild = categoryCommandService.createCategory(new CategoryRequest("Grandchild Cat", child.id(), null));

        // Attempt to make Parent a child of Grandchild
        CategoryRequest invalidUpdate = new CategoryRequest("Parent Moved Down", grandchild.id(), null);
        CoreException ex = assertThrows(CoreException.class, () -> categoryCommandService.updateCategory(parent.id(), invalidUpdate));
        assertEquals(ResponseCode.CATEGORY_CYCLE_DETECTED, ex.getResponseCode());
    }

    @Test
    @DisplayName("TC-05: Category cascade deactivation - Should deactivate category and all its descendants")
    void testCategoryCascadeDeactivation() {
        CategoryResponse c1 = categoryCommandService.createCategory(new CategoryRequest("Parent To Delete", null, null));
        CategoryResponse c2 = categoryCommandService.createCategory(new CategoryRequest("Child To Deactivate", c1.id(), null));
        CategoryResponse c3 = categoryCommandService.createCategory(new CategoryRequest("Grandchild To Deactivate", c2.id(), null));

        categoryCommandService.deleteCategory(c1.id());

        CategoryEntity e1 = categoryRepository.findById(c1.id()).orElseThrow();
        CategoryEntity e2 = categoryRepository.findById(c2.id()).orElseThrow();
        CategoryEntity e3 = categoryRepository.findById(c3.id()).orElseThrow();

        assertFalse(e1.isActive(), "Parent must be inactive");
        assertFalse(e2.isActive(), "Child must be cascade deactivated");
        assertFalse(e3.isActive(), "Grandchild must be cascade deactivated");

        List<CategoryTreeResponse> activeTree = categoryQueryService.getCategoryTree();
        assertTrue(activeTree.isEmpty(), "Active tree should not contain deactivated branches");
    }

    @Test
    @DisplayName("TC-01: Category tree - Should build nested hierarchy of active categories")
    void testCategoryTreeStructure() {
        CategoryResponse cat1 = categoryCommandService.createCategory(new CategoryRequest("Computers", null, null));
        categoryCommandService.createCategory(new CategoryRequest("Laptops", cat1.id(), null));
        categoryCommandService.createCategory(new CategoryRequest("Desktops", cat1.id(), null));

        CategoryResponse cat2 = categoryCommandService.createCategory(new CategoryRequest("Home Appliances", null, null));
        categoryCommandService.createCategory(new CategoryRequest("Microwaves", cat2.id(), null));

        List<CategoryTreeResponse> tree = categoryQueryService.getCategoryTree();

        assertEquals(2, tree.size(), "Should have 2 root categories");
        CategoryTreeResponse compRoot = tree.stream().filter(t -> t.name().equals("Computers")).findFirst().orElseThrow();
        assertEquals(2, compRoot.children().size(), "Computers should have 2 children");

        CategoryTreeResponse homeRoot = tree.stream().filter(t -> t.name().equals("Home Appliances")).findFirst().orElseThrow();
        assertEquals(1, homeRoot.children().size(), "Home Appliances should have 1 child");
    }

    @Test
    @DisplayName("TC-06 & TC-07: Brand creation and duplicate prevention - Should prevent duplicate brand names")
    void testBrandCreationAndDuplicatePrevention() {
        BrandRequest req = new BrandRequest("Apple Inc", "Cupertino tech giant", "https://img.techmart.vn/apple.png");
        BrandResponse response = brandCommandService.createBrand(req);

        assertNotNull(response.id());
        assertEquals("Apple Inc", response.name());
        assertEquals("https://img.techmart.vn/apple.png", response.logoUrl());

        List<BrandResponse> brands = brandQueryService.getBrandList();
        assertEquals(1, brands.size());

        // Duplicate brand creation (case-insensitive) must throw DUPLICATE_RESOURCE
        BrandRequest dupReq = new BrandRequest("apple inc", "Duplicate description", null);
        CoreException ex = assertThrows(CoreException.class, () -> brandCommandService.createBrand(dupReq));
        assertEquals(ResponseCode.DUPLICATE_RESOURCE, ex.getResponseCode());
    }

    @Test
    @DisplayName("TC-08 & TC-09: Branch creation & masked listing - Should create branch with 0 quantityAvailable and prevent duplicate")
    void testBranchCreationAndListing() {
        BranchRequest req = new BranchRequest("TechMart District 1", "123 Le Loi, Dist 1, HCMC", "02812345678");
        BranchResponse response = branchCommandService.createBranch(req);

        assertNotNull(response.id());
        assertEquals("TechMart District 1", response.name());
        assertEquals(0, response.quantityAvailable());

        List<BranchResponse> branches = branchQueryService.getBranchList();
        assertEquals(1, branches.size());
        assertEquals("TechMart District 1", branches.get(0).name());
        assertEquals(0, branches.get(0).quantityAvailable());

        // Duplicate branch name
        BranchRequest dupReq = new BranchRequest("techmart district 1", "456 Nguyen Hue", "02899999999");
        CoreException ex = assertThrows(CoreException.class, () -> branchCommandService.createBranch(dupReq));
        assertEquals(ResponseCode.DUPLICATE_RESOURCE, ex.getResponseCode());
    }

    @Test
    @DisplayName("TC-13: Product creation & duplicate slug - Should create product with primary image and reject duplicate slug")
    void testProductCreationAndDuplicateSlug() {
        BrandResponse brand = brandCommandService.createBrand(new BrandRequest("Sony", "Sony Corp", null));
        CategoryResponse cat = categoryCommandService.createCategory(new CategoryRequest("Headphones", null, null));

        List<ProductImageRequest> images = List.of(
                new ProductImageRequest("https://img.techmart.vn/wh1000xm5-main.jpg", true, 0),
                new ProductImageRequest("https://img.techmart.vn/wh1000xm5-side.jpg", false, 1)
        );

        ProductCreateRequest prodReq = new ProductCreateRequest(
                "Sony WH-1000XM5 Wireless Headphones",
                "sony-wh1000xm5",
                "Noise cancelling headphones",
                "https://img.techmart.vn/wh1000xm5-main.jpg",
                cat.id(),
                brand.id(),
                images
        );

        ProductDetailResponse prod = productCommandService.createProduct(prodReq);

        assertNotNull(prod.id());
        assertEquals("sony-wh1000xm5", prod.slug());
        assertEquals(2, prod.images().size());
        assertTrue(prod.images().get(0).isPrimary(), "First image must be primary");
        assertFalse(prod.images().get(1).isPrimary(), "Second image must not be primary");

        // Duplicate slug
        ProductCreateRequest dupSlugReq = new ProductCreateRequest(
                "Another Sony Headphone",
                "sony-wh1000xm5",
                "Duplicate slug test",
                null,
                cat.id(),
                brand.id(),
                null
        );

        CoreException ex = assertThrows(CoreException.class, () -> productCommandService.createProduct(dupSlugReq));
        assertEquals(ResponseCode.DUPLICATE_RESOURCE, ex.getResponseCode());
    }

    @Test
    @DisplayName("TC-14: Variant creation & initial stock - Should create variant, initialize inventory, and record history")
    void testVariantCreationWithInitialStock() {
        BrandResponse brand = brandCommandService.createBrand(new BrandRequest("Samsung", "Korean tech leader", null));
        CategoryResponse cat = categoryCommandService.createCategory(new CategoryRequest("Flagships", null, null));
        BranchResponse branch = branchCommandService.createBranch(new BranchRequest("Branch Q7", "Nguyen Van Linh, Q7", "02877777777"));

        ProductCreateRequest prodReq = new ProductCreateRequest(
                "Samsung Galaxy S24 Ultra",
                null, // slug will be auto-generated
                "Galaxy AI flagship",
                "https://img.techmart.vn/s24u.jpg",
                cat.id(),
                brand.id(),
                null
        );
        ProductDetailResponse prod = productCommandService.createProduct(prodReq);

        VariantCreateRequest variantReq = new VariantCreateRequest(
                "S24U-TITAN-256",
                Map.of("color", "Titan Black", "storage", "256GB"),
                new BigDecimal("29990000.00"),
                new BigDecimal("10.00"), // 10% discount
                "https://img.techmart.vn/s24u-titan.jpg",
                List.of(new InitialStockRequest(branch.id(), 20, 5))
        );

        VariantDetailResponse variant = productCommandService.addVariant(prod.id(), variantReq);

        assertNotNull(variant.id());
        assertEquals("S24U-TITAN-256", variant.sku());
        assertEquals(new BigDecimal("26991000.00"), variant.effectivePrice());
        assertEquals(20, variant.totalAvailableStock());
        assertEquals(1, variant.stockDistribution().size());
        assertEquals(20, variant.stockDistribution().get(0).availableQuantity());

        // Check Branch total stock aggregated
        BranchEntity branchEntity = branchRepository.findById(branch.id()).orElseThrow();
        assertEquals(20, branchEntity.getQuantityAvailable(), "Branch quantityAvailable must reflect initial stock");

        // Check Inventory record
        InventoryEntity inv = inventoryRepository.findByBranchIdAndVariantId(branch.id(), variant.id()).orElseThrow();
        assertEquals(20, inv.getQuantityOnHand());
        assertEquals(0, inv.getQuantityReserved());
        assertEquals(5, inv.getReorderLevel());

        // Check Inventory History
        List<InventoryHistoryEntity> history = inventoryHistoryRepository.findAll();
        assertEquals(1, history.size());
        assertEquals("STOCK_IN", history.get(0).getChangeType());
        assertEquals(20, history.get(0).getQuantityChange());

        // Duplicate SKU must fail
        VariantCreateRequest dupSkuReq = new VariantCreateRequest(
                "S24U-TITAN-256",
                Map.of("color", "Titan Gray"),
                new BigDecimal("29990000.00"),
                BigDecimal.ZERO,
                null,
                null
        );
        CoreException dupEx = assertThrows(CoreException.class, () -> productCommandService.addVariant(prod.id(), dupSkuReq));
        assertEquals(ResponseCode.DUPLICATE_RESOURCE, dupEx.getResponseCode());
    }

    @Test
    @DisplayName("TC-11: Product search & tree filter - Should find products across descendant categories using Materialized Path")
    void testProductCatalogSearchAndHierarchyFilter() {
        BrandResponse brand = brandCommandService.createBrand(new BrandRequest("Dell", "Dell Computers", null));
        CategoryResponse rootCat = categoryCommandService.createCategory(new CategoryRequest("IT Equipment", null, null));
        CategoryResponse childCat = categoryCommandService.createCategory(new CategoryRequest("Notebooks", rootCat.id(), null));
        CategoryResponse leafCat = categoryCommandService.createCategory(new CategoryRequest("Gaming Laptops", childCat.id(), null));

        BranchResponse branch = branchCommandService.createBranch(new BranchRequest("Center Branch", "1 Hai Ba Trung", "02811111111"));

        // Product in leaf category
        ProductCreateRequest p1Req = new ProductCreateRequest(
                "Dell Alienware M16",
                "dell-alienware-m16",
                "Powerful gaming laptop",
                "https://img.techmart.vn/m16.jpg",
                leafCat.id(),
                brand.id(),
                null
        );
        ProductDetailResponse p1 = productCommandService.createProduct(p1Req);
        productCommandService.addVariant(p1.id(), new VariantCreateRequest(
                "DELL-AW-M16",
                Map.of("gpu", "RTX 4080"),
                new BigDecimal("55000000.00"),
                BigDecimal.ZERO,
                null,
                List.of(new InitialStockRequest(branch.id(), 5, 2))
        ));

        // Product in root category (no stock)
        ProductCreateRequest p2Req = new ProductCreateRequest(
                "Dell Server PowerEdge",
                "dell-poweredge",
                "Enterprise server",
                "https://img.techmart.vn/server.jpg",
                rootCat.id(),
                brand.id(),
                null
        );
        ProductDetailResponse p2 = productCommandService.createProduct(p2Req);
        productCommandService.addVariant(p2.id(), new VariantCreateRequest(
                "DELL-PE-R750",
                Map.of("cpu", "Xeon"),
                new BigDecimal("80000000.00"),
                BigDecimal.ZERO,
                null,
                List.of(new InitialStockRequest(branch.id(), 0, 1))
        ));

        // 1. Search by root category ID -> should return both p1 (in leaf descendant) and p2 (in root)
        PagedResponse<ProductSummaryResponse> rootSearchResults = productQueryService.searchProducts(
                new SearchProductsQuery(null, rootCat.id(), null, null, null, null, null, PageRequest.of(0, 10))
        );
        assertEquals(2, rootSearchResults.getTotalElements(), "Root search must include descendant products via path prefix");

        // 2. Search by leaf category ID -> should return only p1
        PagedResponse<ProductSummaryResponse> leafSearchResults = productQueryService.searchProducts(
                new SearchProductsQuery(null, leafCat.id(), null, null, null, null, null, PageRequest.of(0, 10))
        );
        assertEquals(1, leafSearchResults.getTotalElements());
        assertEquals("Dell Alienware M16", leafSearchResults.getContent().get(0).name());

        // 3. Search inStockOnly = true -> should return only p1
        PagedResponse<ProductSummaryResponse> inStockResults = productQueryService.searchProducts(
                new SearchProductsQuery(null, null, null, null, null, null, true, PageRequest.of(0, 10))
        );
        assertEquals(1, inStockResults.getTotalElements());
        assertEquals("Dell Alienware M16", inStockResults.getContent().get(0).name());
        assertEquals("IN_STOCK", inStockResults.getContent().get(0).availability());

        // 4. Search by price range [60M, 90M] -> should return p2
        PagedResponse<ProductSummaryResponse> priceFiltered = productQueryService.searchProducts(
                new SearchProductsQuery(null, null, null, null, new BigDecimal("60000000"), new BigDecimal("90000000"), false, PageRequest.of(0, 10))
        );
        assertEquals(1, priceFiltered.getTotalElements());
        assertEquals("Dell Server PowerEdge", priceFiltered.getContent().get(0).name());
    }

    @Test
    @DisplayName("TC-12: Product detail & stock distribution - Should return complete detail with breadcrumbs and branch availability")
    void testProductDetailAndStockDistribution() {
        BrandResponse brand = brandCommandService.createBrand(new BrandRequest("Asus", "Asus ROG", null));
        CategoryResponse cat1 = categoryCommandService.createCategory(new CategoryRequest("Hardware", null, null));
        CategoryResponse cat2 = categoryCommandService.createCategory(new CategoryRequest("Monitors", cat1.id(), null));

        BranchResponse b1 = branchCommandService.createBranch(new BranchRequest("Branch Hanoi", "Cau Giay, HN", "02412345678"));
        BranchResponse b2 = branchCommandService.createBranch(new BranchRequest("Branch Danang", "Hai Chau, DN", "02312345678"));

        ProductCreateRequest prodReq = new ProductCreateRequest(
                "Asus ROG Swift OLED PG27AQDM",
                "asus-rog-pg27aqdm",
                "OLED gaming monitor 240Hz",
                "https://img.techmart.vn/pg27-thumb.jpg",
                cat2.id(),
                brand.id(),
                List.of(new ProductImageRequest("https://img.techmart.vn/pg27-1.jpg", true, 0))
        );
        ProductDetailResponse createdProd = productCommandService.createProduct(prodReq);

        productCommandService.addVariant(createdProd.id(), new VariantCreateRequest(
                "ROG-PG27-OLED",
                Map.of("refresh_rate", "240Hz", "panel", "OLED"),
                new BigDecimal("25000000.00"),
                new BigDecimal("20.00"),
                null,
                List.of(
                        new InitialStockRequest(b1.id(), 8, 2),
                        new InitialStockRequest(b2.id(), 4, 1)
                )
        ));

        // Query product detail by slug
        ProductDetailResponse detail = productQueryService.getProductBySlug("asus-rog-pg27aqdm");

        assertEquals("Asus ROG Swift OLED PG27AQDM", detail.name());
        assertEquals("hardware > monitors", detail.category().breadcrumb());
        assertEquals(1, detail.variants().size());

        VariantDetailResponse variant = detail.variants().get(0);
        assertEquals("ROG-PG27-OLED", variant.sku());
        assertEquals(new BigDecimal("20000000.00"), variant.effectivePrice(), "20% discount on 25M gives 20M");
        assertEquals(12, variant.totalAvailableStock());
        assertEquals(2, variant.stockDistribution().size());
    }

    @Test
    @DisplayName("TC-10: Branch inventory - Should return paginated inventory items for staff/admin")
    void testBranchInternalInventory() {
        BrandResponse brand = brandCommandService.createBrand(new BrandRequest("Logitech", "Peripherals", null));
        CategoryResponse cat = categoryCommandService.createCategory(new CategoryRequest("Mice", null, null));
        BranchResponse branch = branchCommandService.createBranch(new BranchRequest("Branch HCM", "Dist 3", "02833333333"));

        ProductCreateRequest prodReq = new ProductCreateRequest("Logitech MX Master 3S", null, "Ergonomic mouse", null, cat.id(), brand.id(), null);
        ProductDetailResponse prod = productCommandService.createProduct(prodReq);

        productCommandService.addVariant(prod.id(), new VariantCreateRequest(
                "MX-MASTER-3S-GR",
                Map.of("color", "Graphite"),
                new BigDecimal("2490000.00"),
                BigDecimal.ZERO,
                null,
                List.of(new InitialStockRequest(branch.id(), 35, 5))
        ));

        PagedResponse<BranchInventoryItemResponse> branchInv = branchQueryService.getBranchInventory(branch.id(), PageRequest.of(0, 10));

        assertEquals(1, branchInv.getTotalElements());
        BranchInventoryItemResponse item = branchInv.getContent().get(0);
        assertEquals("MX-MASTER-3S-GR", item.variantSku());
        assertEquals("Logitech MX Master 3S", item.productName());
        assertEquals(35, item.quantityOnHand());
        assertEquals(0, item.quantityReserved());
        assertEquals(35, item.availableQuantity());
        assertEquals(5, item.reorderLevel());
    }

    @Test
    @DisplayName("TC-15: Inventory adjustment - Should successfully handle STOCK_IN and update branch total")
    void testInventoryAdjustStockIn() {
        BrandResponse brand = brandCommandService.createBrand(new BrandRequest("Anker", "Charging", null));
        CategoryResponse cat = categoryCommandService.createCategory(new CategoryRequest("Accessories", null, null));
        BranchResponse branch = branchCommandService.createBranch(new BranchRequest("Stock Branch", "Address", "0123456789"));

        ProductCreateRequest prodReq = new ProductCreateRequest("Anker 737 Power Bank", null, "PowerBank", null, cat.id(), brand.id(), null);
        ProductDetailResponse prod = productCommandService.createProduct(prodReq);

        VariantDetailResponse variant = productCommandService.addVariant(prod.id(), new VariantCreateRequest(
                "ANKER-737",
                Map.of("capacity", "24000mAh"),
                new BigDecimal("2990000.00"),
                BigDecimal.ZERO,
                null,
                List.of(new InitialStockRequest(branch.id(), 10, 2))
        ));

        // Adjust stock in +15
        InventoryAdjustRequest adjustReq = new InventoryAdjustRequest(
                branch.id(),
                variant.id(),
                15,
                "STOCK_IN",
                "New shipment arrived"
        );
        InventoryAdjustResponse response = inventoryCommandService.adjustInventory(adjustReq);

        assertEquals(10, response.quantityOnHandBefore());
        assertEquals(25, response.quantityOnHandAfter());
        assertEquals(25, response.availableQuantity());

        BranchEntity updatedBranch = branchRepository.findById(branch.id()).orElseThrow();
        assertEquals(25, updatedBranch.getQuantityAvailable());

        List<InventoryHistoryEntity> historyList = inventoryHistoryRepository.findAll();
        assertEquals(2, historyList.size(), "1 for initial stock + 1 for adjustment");
    }

    @Test
    @DisplayName("TC-15: Anti-negative stock constraint - Should reject reduction that breaches reserved stock or drops below 0")
    void testInventoryAdjustAntiNegativeStock() {
        BrandResponse brand = brandCommandService.createBrand(new BrandRequest("Keychron", "Keyboards", null));
        CategoryResponse cat = categoryCommandService.createCategory(new CategoryRequest("Keyboards", null, null));
        BranchResponse branch = branchCommandService.createBranch(new BranchRequest("Keychron Branch", "Address", "0123456789"));

        ProductCreateRequest prodReq = new ProductCreateRequest("Keychron Q1 Pro", null, "Custom keyboard", null, cat.id(), brand.id(), null);
        ProductDetailResponse prod = productCommandService.createProduct(prodReq);

        VariantDetailResponse variant = productCommandService.addVariant(prod.id(), new VariantCreateRequest(
                "KEYCHRON-Q1-PRO",
                Map.of("switch", "Red"),
                new BigDecimal("4500000.00"),
                BigDecimal.ZERO,
                null,
                List.of(new InitialStockRequest(branch.id(), 10, 2))
        ));

        // Simulate 5 items currently reserved by customers
        InventoryEntity inv = inventoryRepository.findByBranchIdAndVariantId(branch.id(), variant.id()).orElseThrow();
        inv.setQuantityReserved(5);
        inventoryRepository.save(inv);

        // Attempt STOCK_OUT of 6 items (newOnHand = 10 - 6 = 4 < reserved = 5) -> MUST FAIL
        InventoryAdjustRequest stockOutTooMuch = new InventoryAdjustRequest(
                branch.id(),
                variant.id(),
                6,
                "STOCK_OUT",
                "Reduce more than unreserved"
        );
        CoreException ex = assertThrows(CoreException.class, () -> inventoryCommandService.adjustInventory(stockOutTooMuch));
        assertEquals(ResponseCode.INSUFFICIENT_STOCK, ex.getResponseCode());

        // Attempt negative adjustment dropping below 0
        InventoryAdjustRequest adjustBelowZero = new InventoryAdjustRequest(
                branch.id(),
                variant.id(),
                -20,
                "ADJUSTMENT",
                "Dropping below 0"
        );
        CoreException negEx = assertThrows(CoreException.class, () -> inventoryCommandService.adjustInventory(adjustBelowZero));
        assertEquals(ResponseCode.INSUFFICIENT_STOCK, negEx.getResponseCode());
    }

    @Test
    @DisplayName("TC-16: Core security - Should reject requests missing X-Internal-Secret with 403 INVALID_INTERNAL_SECRET")
    void testCoreSecurityMissingInternalSecret() throws Exception {
        mockMvc.perform(get("/internal/api/v1/categories/tree")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ResponseCode.INVALID_INTERNAL_SECRET.getCode()))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.timestamp").value(notNullValue()));
    }

    @Test
    @DisplayName("TC-16: Core security - Should reject CUSTOMER role attempting ADMIN action with 403 ACCESS_DENIED")
    void testCoreSecurityRoleAccessDenied() throws Exception {
        UUID customerId = UUID.randomUUID();
        CategoryRequest req = new CategoryRequest("Hacker Category", null, "Should be blocked");

        mockMvc.perform(post("/internal/api/v1/categories")
                        .header(HeaderConstants.X_INTERNAL_SECRET, VALID_INTERNAL_SECRET)
                        .header("X-User-Id", customerId.toString())
                        .header("X-User-Role", "CUSTOMER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ResponseCode.ACCESS_DENIED.getCode()));
    }

    @Test
    @DisplayName("TC-16: Core security - Should permit ADMIN role to create category with valid secret")
    void testCoreSecurityAdminAccessAllowed() throws Exception {
        UUID adminId = UUID.randomUUID();
        CategoryRequest req = new CategoryRequest("Smart Home Devices", null, "Authorized creation");

        mockMvc.perform(post("/internal/api/v1/categories")
                        .header(HeaderConstants.X_INTERNAL_SECRET, VALID_INTERNAL_SECRET)
                        .header("X-User-Id", adminId.toString())
                        .header("X-User-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Smart Home Devices"))
                .andExpect(jsonPath("$.level").value(1));
    }
}
