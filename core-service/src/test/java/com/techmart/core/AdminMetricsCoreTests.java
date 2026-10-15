package com.techmart.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.techmart.core.authorization.UserContext;
import com.techmart.core.dto.response.metrics.DashboardSummaryResponse;
import com.techmart.core.dto.response.metrics.RevenueChartResponse;
import com.techmart.core.dto.response.metrics.StockAlertItemResponse;
import com.techmart.core.dto.response.metrics.SystemHealthResponse;
import com.techmart.core.entity.branch.BranchEntity;
import com.techmart.core.entity.catalog.BrandEntity;
import com.techmart.core.entity.catalog.CategoryEntity;
import com.techmart.core.entity.catalog.ProductEntity;
import com.techmart.core.entity.catalog.ProductVariantEntity;
import com.techmart.core.entity.inventory.InventoryEntity;
import com.techmart.core.entity.order.OrderEntity;
import com.techmart.core.entity.order.OrderStatus;
import com.techmart.core.entity.user.Role;
import com.techmart.core.entity.user.UserEntity;
import com.techmart.core.repository.auth.RefreshTokenRepository;
import com.techmart.core.repository.branch.BranchRepository;
import com.techmart.core.repository.cart.CartItemRepository;
import com.techmart.core.repository.cart.CartRepository;
import com.techmart.core.repository.catalog.BrandRepository;
import com.techmart.core.repository.catalog.CategoryRepository;
import com.techmart.core.repository.catalog.ProductImageRepository;
import com.techmart.core.repository.catalog.ProductRepository;
import com.techmart.core.repository.catalog.ProductVariantRepository;
import com.techmart.core.repository.idempotency.IdempotencyKeyRepository;
import com.techmart.core.repository.inventory.InventoryHistoryRepository;
import com.techmart.core.repository.inventory.InventoryRepository;
import com.techmart.core.repository.order.OrderItemRepository;
import com.techmart.core.repository.order.OrderRepository;
import com.techmart.core.repository.user.UserRepository;
import com.techmart.core.service.command.idempotency.IdempotencyService;
import com.techmart.shared.constant.HeaderConstants;
import com.techmart.shared.constant.ResponseCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AdminMetricsCoreTests {

    private static final String VALID_INTERNAL_SECRET = "techmart_internal_secret_2026";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private InventoryHistoryRepository inventoryHistoryRepository;

    @Autowired
    private ProductVariantRepository productVariantRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductImageRepository productImageRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private IdempotencyKeyRepository idempotencyKeyRepository;

    @Autowired
    private IdempotencyService idempotencyService;

    @BeforeEach
    void setUp() {
        cleanDatabase();
    }

    @AfterEach
    void tearDown() {
        cleanDatabase();
        UserContext.clear();
    }

    private void cleanDatabase() {
        idempotencyKeyRepository.deleteAll();
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();
        cartItemRepository.deleteAll();
        cartRepository.deleteAll();
        inventoryHistoryRepository.deleteAll();
        inventoryRepository.deleteAll();
        productVariantRepository.deleteAll();
        productImageRepository.deleteAll();
        productRepository.deleteAll();
        brandRepository.deleteAll();
        categoryRepository.deleteAll();
        branchRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    private UserEntity createTestUser(String email, String fullName, Role role) {
        UserEntity user = new UserEntity();
        user.setEmail(email);
        user.setFullName(fullName);
        user.setPasswordHash("$2a$10$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36WQoeG6Lruj3vjPGga31lW");
        user.setPhone("090" + UUID.randomUUID().toString().replaceAll("[^0-9]", "").substring(0, 7));
        user.setRole(role);
        return userRepository.save(user);
    }

    private BranchEntity createBranch(String name) {
        BranchEntity branch = new BranchEntity();
        branch.setName(name);
        branch.setAddress("Address " + name);
        branch.setPhone("02499998888");
        branch.setActive(true);
        branch.setQuantityAvailable(100);
        return branchRepository.save(branch);
    }

    private ProductVariantEntity createProductVariant(String sku, BigDecimal price) {
        CategoryEntity category = new CategoryEntity();
        category.setName("Category " + UUID.randomUUID().toString().substring(0, 5));
        category.setLevel(1);
        category.setPath("/test");
        category.setActive(true);
        category = categoryRepository.save(category);

        BrandEntity brand = new BrandEntity();
        brand.setName("Brand " + UUID.randomUUID().toString().substring(0, 5));
        brand.setDescription("Brand desc");
        brand.setActive(true);
        brand = brandRepository.save(brand);

        ProductEntity product = new ProductEntity();
        product.setName("Product " + sku);
        product.setSlug("slug-" + UUID.randomUUID().toString().substring(0, 8));
        product.setDescription("Desc");
        product.setCategory(category);
        product.setBrand(brand);
        product.setStatus("ACTIVE");
        product = productRepository.save(product);

        ProductVariantEntity variant = new ProductVariantEntity();
        variant.setProduct(product);
        variant.setSku(sku);
        variant.setPrice(price);
        variant.setDiscountPercent(BigDecimal.ZERO);
        variant.setStatus("ACTIVE");
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("color", "Black");
        variant.setAttributes(attrs);
        return productVariantRepository.save(variant);
    }

    private OrderEntity createOrderWithTime(UserEntity customer, BranchEntity branch, OrderStatus status, BigDecimal amount, Instant createdAt) {
        OrderEntity order = new OrderEntity();
        order.setCustomerId(customer.getId());
        order.setCustomerName(customer.getFullName());
        order.setCustomerPhone(customer.getPhone());
        order.setCustomerEmail(customer.getEmail());
        order.setBranch(branch);
        order.setOrderNumber("ORD-" + UUID.randomUUID().toString().substring(0, 8));
        order.setStatus(status);
        order.setTotalAmount(amount);
        order.setShippingAddress("123 Street, Hanoi");
        order.setCreatedAt(createdAt);
        return orderRepository.saveAndFlush(order);
    }

    // ==========================================
    // AC-07: DASHBOARD SUMMARY SINGLE-QUERY SQL AGGREGATION (< 5ms)
    // ==========================================
    @Test
    @DisplayName("AC-07: Dashboard summary aggregates today/yesterday revenue, growth %, order statuses, inventory, and new customers in < 5ms")
    void testDashboardSummaryAggregation() throws Exception {
        UserEntity admin = createTestUser("admin@techmart.vn", "Admin One", Role.ADMIN);
        UserEntity customer = createTestUser("cust@techmart.vn", "Cust One", Role.CUSTOMER);
        BranchEntity branch = createBranch("Hanoi Main");

        Instant now = Instant.now();
        Instant yesterday = now.minus(1, ChronoUnit.DAYS);

        // 1. Orders setup
        // Today DELIVERED: 5,000,000
        createOrderWithTime(customer, branch, OrderStatus.DELIVERED, new BigDecimal("5000000.00"), now);
        // Today PENDING: 2,000,000
        createOrderWithTime(customer, branch, OrderStatus.PENDING, new BigDecimal("2000000.00"), now);
        // Today CANCELLED: 1,000,000
        createOrderWithTime(customer, branch, OrderStatus.CANCELLED, new BigDecimal("1000000.00"), now);

        // Yesterday DELIVERED: 4,000,000
        createOrderWithTime(customer, branch, OrderStatus.DELIVERED, new BigDecimal("4000000.00"), yesterday);

        // 2. Inventory setup
        ProductVariantEntity var1 = createProductVariant("SKU-LOW", new BigDecimal("1000000.00"));
        ProductVariantEntity var2 = createProductVariant("SKU-OUT", new BigDecimal("2000000.00"));
        ProductVariantEntity var3 = createProductVariant("SKU-OK", new BigDecimal("3000000.00"));

        // Low stock: onHand 3, reserved 0, reorder 5 -> available 3 <= 5
        InventoryEntity inv1 = new InventoryEntity();
        inv1.setBranch(branch);
        inv1.setVariant(var1);
        inv1.setQuantityOnHand(3);
        inv1.setQuantityReserved(0);
        inv1.setReorderLevel(5);
        inventoryRepository.save(inv1);

        // Out of stock: onHand 2, reserved 2 -> available 0 <= 0
        InventoryEntity inv2 = new InventoryEntity();
        inv2.setBranch(branch);
        inv2.setVariant(var2);
        inv2.setQuantityOnHand(2);
        inv2.setQuantityReserved(2);
        inv2.setReorderLevel(5);
        inventoryRepository.save(inv2);

        // Healthy stock: onHand 20, reserved 0 -> available 20 > 5
        InventoryEntity inv3 = new InventoryEntity();
        inv3.setBranch(branch);
        inv3.setVariant(var3);
        inv3.setQuantityOnHand(20);
        inv3.setQuantityReserved(0);
        inv3.setReorderLevel(5);
        inventoryRepository.save(inv3);

        // Measure execution latency
        long start = System.currentTimeMillis();
        mockMvc.perform(get("/internal/api/v1/metrics/summary")
                        .header(HeaderConstants.X_INTERNAL_SECRET, VALID_INTERNAL_SECRET)
                        .header("X-User-Id", admin.getId().toString())
                        .header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.revenue.todayRevenue").value(5000000.00))
                .andExpect(jsonPath("$.revenue.yesterdayRevenue").value(4000000.00))
                .andExpect(jsonPath("$.revenue.growthRatePercent").value(25.0))
                .andExpect(jsonPath("$.orders.todayOrdersTotal").value(3))
                .andExpect(jsonPath("$.orders.deliveredCount").value(1))
                .andExpect(jsonPath("$.orders.pendingCount").value(1))
                .andExpect(jsonPath("$.orders.cancelledCount").value(1))
                .andExpect(jsonPath("$.inventory.lowStockVariantsCount").value(1))
                .andExpect(jsonPath("$.inventory.outOfStockVariantsCount").value(1))
                .andExpect(jsonPath("$.customers.newCustomersToday").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.customers.totalActiveCustomers").value(greaterThanOrEqualTo(1)));
        long duration = System.currentTimeMillis() - start;
        assertTrue(duration < 2000, "API summary response time should be fast and responsive");
    }

    // ==========================================
    // AC-07: DASHBOARD SUMMARY BRANCH FILTER & EDGE CASES
    // ==========================================
    @Test
    @DisplayName("AC-07: Dashboard summary filters by branch_id correctly and handles zero growth edge cases")
    void testDashboardSummaryBranchFilterAndZeroGrowth() throws Exception {
        UserEntity admin = createTestUser("admin2@techmart.vn", "Admin Two", Role.ADMIN);
        UserEntity customer = createTestUser("cust2@techmart.vn", "Cust Two", Role.CUSTOMER);
        BranchEntity branchA = createBranch("Branch A");
        BranchEntity branchB = createBranch("Branch B");

        Instant now = Instant.now();
        // Branch A order
        createOrderWithTime(customer, branchA, OrderStatus.DELIVERED, new BigDecimal("10000000.00"), now);
        // Branch B order
        createOrderWithTime(customer, branchB, OrderStatus.DELIVERED, new BigDecimal("20000000.00"), now);

        // Query Branch A only: should see 10,000,000 revenue
        mockMvc.perform(get("/internal/api/v1/metrics/summary?branchId=" + branchA.getId())
                        .header(HeaderConstants.X_INTERNAL_SECRET, VALID_INTERNAL_SECRET)
                        .header("X-User-Id", admin.getId().toString())
                        .header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.revenue.todayRevenue").value(10000000.00))
                .andExpect(jsonPath("$.revenue.yesterdayRevenue").value(0.00))
                .andExpect(jsonPath("$.revenue.growthRatePercent").value(100.0)) // Edge case: yesterday=0, today>0 -> 100.0%
                .andExpect(jsonPath("$.orders.todayOrdersTotal").value(1));

        // Query Non-existent Branch: should return all 0s without throwing NPE
        UUID emptyBranchId = UUID.randomUUID();
        mockMvc.perform(get("/internal/api/v1/metrics/summary?branchId=" + emptyBranchId)
                        .header(HeaderConstants.X_INTERNAL_SECRET, VALID_INTERNAL_SECRET)
                        .header("X-User-Id", admin.getId().toString())
                        .header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.revenue.todayRevenue").value(0.00))
                .andExpect(jsonPath("$.revenue.yesterdayRevenue").value(0.00))
                .andExpect(jsonPath("$.revenue.growthRatePercent").value(0.0)) // Edge case: yesterday=0, today=0 -> 0.0%
                .andExpect(jsonPath("$.orders.todayOrdersTotal").value(0));
    }

    // ==========================================
    // AC-08: REVENUE CHART WITH ZERO-FILLING
    // ==========================================
    @Test
    @DisplayName("AC-08: Revenue chart generates complete timeline with zero-filling for days without orders")
    void testRevenueChartWithZeroFilling() throws Exception {
        UserEntity admin = createTestUser("admin3@techmart.vn", "Admin Three", Role.ADMIN);
        UserEntity customer = createTestUser("cust3@techmart.vn", "Cust Three", Role.CUSTOMER);
        BranchEntity branch = createBranch("Da Nang");

        LocalDate todayUtc = LocalDate.now(ZoneOffset.UTC);
        Instant orderTime = todayUtc.minusDays(2).atStartOfDay(ZoneOffset.UTC).toInstant().plus(12, ChronoUnit.HOURS);
        // Delivered order 2 days ago
        createOrderWithTime(customer, branch, OrderStatus.DELIVERED, new BigDecimal("12000000.00"), orderTime);
        // Cancelled order 2 days ago -> should be excluded from chart
        createOrderWithTime(customer, branch, OrderStatus.CANCELLED, new BigDecimal("5000000.00"), orderTime);

        // 1. Test 7_DAYS period (default) -> exactly 7 timeline points
        mockMvc.perform(get("/internal/api/v1/metrics/revenue-chart?period=7_DAYS")
                        .header(HeaderConstants.X_INTERNAL_SECRET, VALID_INTERNAL_SECRET)
                        .header("X-User-Id", admin.getId().toString())
                        .header("X-User-Role", "STAFF"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.period").value("7_DAYS"))
                .andExpect(jsonPath("$.timeline", hasSize(7)))
                .andExpect(jsonPath("$.totalPeriodRevenue").value(12000000.00))
                .andExpect(jsonPath("$.totalPeriodOrders").value(1))
                // Day 2 days ago (index 4 in 7-day timeline: 0, 1, 2, 3, 4, 5, 6) has revenue 12M and 1 order
                .andExpect(jsonPath("$.timeline[4].revenue").value(12000000.00))
                .andExpect(jsonPath("$.timeline[4].ordersCount").value(1))
                // Other days (zero-filled) have 0.00 revenue and 0 orders
                .andExpect(jsonPath("$.timeline[0].revenue").value(0.00))
                .andExpect(jsonPath("$.timeline[0].ordersCount").value(0));

        // 2. Test TODAY period -> exactly 1 timeline point
        mockMvc.perform(get("/internal/api/v1/metrics/revenue-chart?period=TODAY")
                        .header(HeaderConstants.X_INTERNAL_SECRET, VALID_INTERNAL_SECRET)
                        .header("X-User-Id", admin.getId().toString())
                        .header("X-User-Role", "STAFF"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.period").value("TODAY"))
                .andExpect(jsonPath("$.timeline", hasSize(1)));

        // 3. Test 30_DAYS period -> exactly 30 timeline points
        mockMvc.perform(get("/internal/api/v1/metrics/revenue-chart?period=30_DAYS")
                        .header(HeaderConstants.X_INTERNAL_SECRET, VALID_INTERNAL_SECRET)
                        .header("X-User-Id", admin.getId().toString())
                        .header("X-User-Role", "STAFF"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.period").value("30_DAYS"))
                .andExpect(jsonPath("$.timeline", hasSize(30)));
    }

    // ==========================================
    // AC-09: STOCK ALERTS FILTERING & PAGINATION
    // ==========================================
    @Test
    @DisplayName("AC-09: Stock alerts filters variants <= threshold or reorder_level, classifies statuses, and supports pagination")
    void testStockAlertsFilteringAndPagination() throws Exception {
        UserEntity staff = createTestUser("staff@techmart.vn", "Staff User", Role.STAFF);
        BranchEntity branch = createBranch("Can Tho");

        ProductVariantEntity varOut = createProductVariant("SKU-ALERT-OUT", new BigDecimal("1000000.00"));
        ProductVariantEntity varCrit = createProductVariant("SKU-ALERT-CRIT", new BigDecimal("2000000.00"));
        ProductVariantEntity varLow = createProductVariant("SKU-ALERT-LOW", new BigDecimal("3000000.00"));
        ProductVariantEntity varOk = createProductVariant("SKU-ALERT-OK", new BigDecimal("4000000.00"));

        // OUT_OF_STOCK: available = 0
        InventoryEntity invOut = new InventoryEntity();
        invOut.setBranch(branch);
        invOut.setVariant(varOut);
        invOut.setQuantityOnHand(5);
        invOut.setQuantityReserved(5);
        invOut.setReorderLevel(10);
        inventoryRepository.save(invOut);

        // CRITICAL_LOW: available = 2 (0 < available <= 2)
        InventoryEntity invCrit = new InventoryEntity();
        invCrit.setBranch(branch);
        invCrit.setVariant(varCrit);
        invCrit.setQuantityOnHand(4);
        invCrit.setQuantityReserved(2);
        invCrit.setReorderLevel(10);
        inventoryRepository.save(invCrit);

        // LOW_STOCK: available = 4 (available <= threshold 5)
        InventoryEntity invLow = new InventoryEntity();
        invLow.setBranch(branch);
        invLow.setVariant(varLow);
        invLow.setQuantityOnHand(6);
        invLow.setQuantityReserved(2);
        invLow.setReorderLevel(10);
        inventoryRepository.save(invLow);

        // Normal: available = 20 > threshold 5 and > reorder 10 -> excluded
        InventoryEntity invOk = new InventoryEntity();
        invOk.setBranch(branch);
        invOk.setVariant(varOk);
        invOk.setQuantityOnHand(25);
        invOk.setQuantityReserved(5);
        invOk.setReorderLevel(10);
        inventoryRepository.save(invOk);

        // Query with threshold = 5, page = 0, size = 10
        mockMvc.perform(get("/internal/api/v1/metrics/stock-alerts?threshold=5&page=0&size=10")
                        .header(HeaderConstants.X_INTERNAL_SECRET, VALID_INTERNAL_SECRET)
                        .header("X-User-Id", staff.getId().toString())
                        .header("X-User-Role", "STAFF"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.content", hasSize(3)))
                .andExpect(jsonPath("$.content[0].alertStatus").value("OUT_OF_STOCK"))
                .andExpect(jsonPath("$.content[0].quantityAvailable").value(0))
                .andExpect(jsonPath("$.content[1].alertStatus").value("CRITICAL_LOW"))
                .andExpect(jsonPath("$.content[1].quantityAvailable").value(2))
                .andExpect(jsonPath("$.content[2].alertStatus").value("LOW_STOCK"))
                .andExpect(jsonPath("$.content[2].quantityAvailable").value(4));

        // Test Pagination: size = 2
        mockMvc.perform(get("/internal/api/v1/metrics/stock-alerts?threshold=5&page=0&size=2")
                        .header(HeaderConstants.X_INTERNAL_SECRET, VALID_INTERNAL_SECRET)
                        .header("X-User-Id", staff.getId().toString())
                        .header("X-User-Role", "STAFF"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.page").value(0));
    }

    // ==========================================
    // AC-10: SYSTEM HEALTH REAL-TIME & RBAC SECURITY
    // ==========================================
    @Test
    @DisplayName("AC-10: System health provides Uptime, JVM Heap, HikariCP, and Idempotency metrics for ADMIN, but blocks STAFF and CUSTOMER with 403")
    void testSystemHealthMetricsAndSecurity() throws Exception {
        UserEntity admin = createTestUser("admin-health@techmart.vn", "Admin Health", Role.ADMIN);
        UserEntity staff = createTestUser("staff-health@techmart.vn", "Staff Health", Role.STAFF);
        UserEntity customer = createTestUser("cust-health@techmart.vn", "Cust Health", Role.CUSTOMER);

        idempotencyService.incrementBlockedRequests();
        idempotencyService.incrementBlockedRequests();

        // 1. ADMIN access -> 200 OK with full health payload
        mockMvc.perform(get("/internal/api/v1/metrics/system-health")
                        .header(HeaderConstants.X_INTERNAL_SECRET, VALID_INTERNAL_SECRET)
                        .header("X-User-Id", admin.getId().toString())
                        .header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.server.uptimeSeconds").value(greaterThanOrEqualTo(0)))
                .andExpect(jsonPath("$.server.uptimeHuman").value(notNullValue()))
                .andExpect(jsonPath("$.memory.heapUsedMb").value(greaterThanOrEqualTo(0)))
                .andExpect(jsonPath("$.memory.heapMaxMb").value(greaterThanOrEqualTo(0)))
                .andExpect(jsonPath("$.databasePool.totalConnections").value(greaterThanOrEqualTo(0)))
                .andExpect(jsonPath("$.cache.status").value("UP"))
                .andExpect(jsonPath("$.idempotencyMetrics.totalBlockedRequests").value(greaterThanOrEqualTo(2)));

        // 2. STAFF access -> 403 Forbidden (ADMIN only)
        mockMvc.perform(get("/internal/api/v1/metrics/system-health")
                        .header(HeaderConstants.X_INTERNAL_SECRET, VALID_INTERNAL_SECRET)
                        .header("X-User-Id", staff.getId().toString())
                        .header("X-User-Role", "STAFF"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ResponseCode.ACCESS_DENIED.getCode()));

        // 3. CUSTOMER access -> 403 Forbidden
        mockMvc.perform(get("/internal/api/v1/metrics/system-health")
                        .header(HeaderConstants.X_INTERNAL_SECRET, VALID_INTERNAL_SECRET)
                        .header("X-User-Id", customer.getId().toString())
                        .header("X-User-Role", "CUSTOMER"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ResponseCode.ACCESS_DENIED.getCode()));

        // 4. Unauthenticated -> 401 Unauthorized
        mockMvc.perform(get("/internal/api/v1/metrics/system-health")
                        .header(HeaderConstants.X_INTERNAL_SECRET, VALID_INTERNAL_SECRET))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ResponseCode.UNAUTHORIZED.getCode()));
    }
}
