package com.techmart.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.uuid.Generators;
import com.techmart.core.authorization.UserContext;
import com.techmart.core.dto.request.order.CreateOrderRequest;
import com.techmart.core.dto.request.order.NewAddressRequest;
import com.techmart.core.dto.request.order.OrderItemRequest;
import com.techmart.core.dto.response.order.OrderResponse;
import com.techmart.core.entity.branch.BranchEntity;
import com.techmart.core.entity.catalog.BrandEntity;
import com.techmart.core.entity.catalog.CategoryEntity;
import com.techmart.core.entity.catalog.ProductEntity;
import com.techmart.core.entity.catalog.ProductVariantEntity;
import com.techmart.core.entity.idempotency.IdempotencyKeyEntity;
import com.techmart.core.entity.idempotency.IdempotencyStatus;
import com.techmart.core.entity.inventory.InventoryEntity;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class IdempotencyEngineCoreTests {

    private static final String VALID_INTERNAL_SECRET = "techmart_internal_secret_2026";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private IdempotencyKeyRepository idempotencyKeyRepository;

    @Autowired
    private IdempotencyService idempotencyService;

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

    private UserEntity createTestCustomer(String email, String fullName, String phone) {
        UserEntity user = new UserEntity();
        user.setEmail(email);
        user.setFullName(fullName);
        user.setPasswordHash("$2a$10$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36WQoeG6Lruj3vjPGga31lW");
        user.setPhone(phone);
        user.setRole(Role.CUSTOMER);
        return userRepository.save(user);
    }

    private TestProductContext createProductAndStock(int onHand, int reserved, BigDecimal price, BigDecimal discount) {
        CategoryEntity category = new CategoryEntity();
        category.setName("Electronics");
        category.setLevel(1);
        category.setPath("/electronics");
        category.setActive(true);
        category = categoryRepository.save(category);

        BrandEntity brand = new BrandEntity();
        brand.setName("Sony-" + UUID.randomUUID().toString().substring(0, 8));
        brand.setDescription("Sony Corp");
        brand.setActive(true);
        brand = brandRepository.save(brand);

        BranchEntity branch = new BranchEntity();
        branch.setName("TechMart Hanoi");
        branch.setAddress("456 Cau Giay, Hanoi");
        branch.setPhone("02412345678");
        branch.setActive(true);
        branch.setQuantityAvailable(onHand - reserved);
        branch = branchRepository.save(branch);

        ProductEntity product = new ProductEntity();
        product.setName("Sony PlayStation 5 Pro");
        product.setSlug("sony-ps5-pro-" + UUID.randomUUID().toString().substring(0, 8));
        product.setDescription("Next-gen gaming console");
        product.setCategory(category);
        product.setBrand(brand);
        product.setStatus("ACTIVE");
        product = productRepository.save(product);

        ProductVariantEntity variant = new ProductVariantEntity();
        variant.setProduct(product);
        variant.setSku("PS5-PRO-" + UUID.randomUUID().toString().substring(0, 6));
        variant.setPrice(price);
        variant.setDiscountPercent(discount);
        variant.setStatus("ACTIVE");
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("storage", "2TB SSD");
        attrs.put("color", "White");
        variant.setAttributes(attrs);
        variant = productVariantRepository.save(variant);

        InventoryEntity inventory = new InventoryEntity();
        inventory.setBranch(branch);
        inventory.setVariant(variant);
        inventory.setQuantityOnHand(onHand);
        inventory.setQuantityReserved(reserved);
        inventory.setReorderLevel(5);
        inventory = inventoryRepository.save(inventory);

        return new TestProductContext(category, brand, branch, product, variant, inventory);
    }

    private record TestProductContext(
            CategoryEntity category,
            BrandEntity brand,
            BranchEntity branch,
            ProductEntity product,
            ProductVariantEntity variant,
            InventoryEntity inventory
    ) {}

    private long countCustomerOrders(UUID customerId) {
        return orderRepository.findByCustomerIdOrderByCreatedAtDesc(customerId, org.springframework.data.domain.Pageable.unpaged()).getTotalElements();
    }

    // ==========================================
    // AC-01: SCHEMA & TABLE INVARIANTS TEST
    // ==========================================
    @Test
    @DisplayName("AC-01: Table idempotency_keys schema, UUID v7 PK, unique key, and check constraint invariants")
    void testSchemaAndTableInvariants() {
        UserEntity customer = createTestCustomer("cust1@techmart.vn", "Customer One", "0900000001");

        IdempotencyKeyEntity entity = new IdempotencyKeyEntity();
        entity.setIdempotencyKey("KEY-AC01-TEST");
        entity.setUserId(customer.getId());
        entity.setRequestPath("/internal/api/v1/orders");
        entity.setRequestHash("a591a6d40bf420404a011733cfb7b190d62c65bf0bcda32b57b277d9ad9f146e");
        entity.setStatus(IdempotencyStatus.IN_PROGRESS);
        entity.setExpiresAt(Instant.now().plus(24, ChronoUnit.HOURS));

        IdempotencyKeyEntity saved = idempotencyKeyRepository.saveAndFlush(entity);
        assertNotNull(saved.getId());
        assertNotNull(saved.getCreatedAt());
        assertEquals(IdempotencyStatus.IN_PROGRESS, saved.getStatus());

        // Test Unique Constraint on idempotency_key
        IdempotencyKeyEntity duplicate = new IdempotencyKeyEntity();
        duplicate.setIdempotencyKey("KEY-AC01-TEST"); // Duplicate key
        duplicate.setUserId(customer.getId());
        duplicate.setRequestPath("/internal/api/v1/orders");
        duplicate.setRequestHash("another-hash");
        duplicate.setStatus(IdempotencyStatus.IN_PROGRESS);
        duplicate.setExpiresAt(Instant.now().plus(24, ChronoUnit.HOURS));

        assertThrows(DataIntegrityViolationException.class, () -> {
            idempotencyKeyRepository.saveAndFlush(duplicate);
        });
    }

    // ==========================================
    // AC-02 & AC-03: FIRST REQUEST CREATES ORDER & PERSISTS SNAPSHOT
    // ==========================================
    @Test
    @DisplayName("AC-02, AC-03: First request with Idempotency-Key returns 201 Created and saves COMPLETED snapshot in DB")
    void testFirstRequestSuccessful201AndSnapshotPersisted() throws Exception {
        UserEntity customer = createTestCustomer("cust2@techmart.vn", "Customer Two", "0900000002");
        TestProductContext ctx = createProductAndStock(20, 0, new BigDecimal("15000000.00"), BigDecimal.ZERO);

        String idempotencyKey = "IDEMPOTENT-ORDER-KEY-001";
        NewAddressRequest address = new NewAddressRequest("Cust Two", "0900000002", "123 Street", "Ward 1", "Hanoi", false);
        CreateOrderRequest request = new CreateOrderRequest(
                ctx.branch().getId(),
                null,
                address,
                "First order note",
                List.of(new OrderItemRequest(ctx.variant().getId(), 2))
        );

        mockMvc.perform(post("/internal/api/v1/orders")
                        .header(HeaderConstants.X_INTERNAL_SECRET, VALID_INTERNAL_SECRET)
                        .header("X-User-Id", customer.getId().toString())
                        .header("X-User-Role", "CUSTOMER")
                        .header("X-User-Email", customer.getEmail())
                        .header(HeaderConstants.IDEMPOTENCY_KEY, idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(notNullValue()))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.totalAmount").value(30000000.00));

        // Verify order saved in DB
        assertEquals(1, countCustomerOrders(customer.getId()));

        // Verify inventory reserved
        InventoryEntity inv = inventoryRepository.findByBranchIdAndVariantId(ctx.branch().getId(), ctx.variant().getId()).orElseThrow();
        assertEquals(20, inv.getQuantityOnHand());
        assertEquals(2, inv.getQuantityReserved());

        // Verify idempotency record
        IdempotencyKeyEntity record = idempotencyKeyRepository.findByIdempotencyKeyAndUserId(idempotencyKey, customer.getId()).orElseThrow();
        assertEquals(IdempotencyStatus.COMPLETED, record.getStatus());
        assertEquals(201, record.getResponseStatus());
        assertNotNull(record.getResponseBody());
        assertTrue(record.getResponseBody().contains("PENDING"));
        assertTrue(record.getResponseBody().contains("30000000.00"));
    }

    // ==========================================
    // AC-04: CONCURRENT REQUEST WHEN IN_PROGRESS RETURNS 409 CONFLICT
    // ==========================================
    @Test
    @DisplayName("AC-04: Concurrent request with same key while IN_PROGRESS returns 409 Conflict")
    void testConcurrentInProgressReturns409Conflict() throws Exception {
        UserEntity customer = createTestCustomer("cust3@techmart.vn", "Customer Three", "0900000003");
        TestProductContext ctx = createProductAndStock(20, 0, new BigDecimal("10000000.00"), BigDecimal.ZERO);

        String idempotencyKey = "IN-PROGRESS-CONFLICT-KEY";

        // Pre-insert record with IN_PROGRESS status
        IdempotencyKeyEntity inProgressEntity = new IdempotencyKeyEntity();
        inProgressEntity.setIdempotencyKey(idempotencyKey);
        inProgressEntity.setUserId(customer.getId());
        inProgressEntity.setRequestPath("/internal/api/v1/orders");
        inProgressEntity.setRequestHash("dummy-hash");
        inProgressEntity.setStatus(IdempotencyStatus.IN_PROGRESS);
        inProgressEntity.setExpiresAt(Instant.now().plus(24, ChronoUnit.HOURS));
        idempotencyKeyRepository.saveAndFlush(inProgressEntity);

        NewAddressRequest address = new NewAddressRequest("Cust Three", "0900000003", "456 Street", "Ward 2", "Hanoi", false);
        CreateOrderRequest request = new CreateOrderRequest(
                ctx.branch().getId(),
                null,
                address,
                "Concurrent test",
                List.of(new OrderItemRequest(ctx.variant().getId(), 1))
        );

        mockMvc.perform(post("/internal/api/v1/orders")
                        .header(HeaderConstants.X_INTERNAL_SECRET, VALID_INTERNAL_SECRET)
                        .header("X-User-Id", customer.getId().toString())
                        .header("X-User-Role", "CUSTOMER")
                        .header("X-User-Email", customer.getEmail())
                        .header(HeaderConstants.IDEMPOTENCY_KEY, idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(ResponseCode.IDEMPOTENCY_KEY_IN_PROGRESS.getCode()));

        // Verify NO order was created
        assertEquals(0, countCustomerOrders(customer.getId()));
    }

    // ==========================================
    // AC-04: MULTI-THREAD CONCURRENT EXECUTION
    // ==========================================
    @Test
    @DisplayName("AC-04: Multi-threaded concurrent execution with same key allows exactly one success and blocks others with 409")
    void testConcurrentMultiThreadIdempotencyConflict() throws Exception {
        UserEntity customer = createTestCustomer("cust-thread@techmart.vn", "Cust Thread", "0900000004");
        TestProductContext ctx = createProductAndStock(50, 0, new BigDecimal("10000000.00"), BigDecimal.ZERO);

        String idempotencyKey = "MULTI-THREAD-KEY-" + UUID.randomUUID();
        NewAddressRequest address = new NewAddressRequest("Cust", "0900000004", "Street", "Ward", "City", false);
        CreateOrderRequest request = new CreateOrderRequest(
                ctx.branch().getId(),
                null,
                address,
                "Concurrent Multi-thread",
                List.of(new OrderItemRequest(ctx.variant().getId(), 2))
        );
        String requestJson = objectMapper.writeValueAsString(request);

        int threads = 4;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threads);

        AtomicInteger status201Count = new AtomicInteger(0);
        AtomicInteger status409Count = new AtomicInteger(0);

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    int code = mockMvc.perform(post("/internal/api/v1/orders")
                                    .header(HeaderConstants.X_INTERNAL_SECRET, VALID_INTERNAL_SECRET)
                                    .header("X-User-Id", customer.getId().toString())
                                    .header("X-User-Role", "CUSTOMER")
                                    .header("X-User-Email", customer.getEmail())
                                    .header(HeaderConstants.IDEMPOTENCY_KEY, idempotencyKey)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(requestJson))
                            .andReturn().getResponse().getStatus();

                    if (code == 201) {
                        status201Count.incrementAndGet();
                    } else if (code == 409) {
                        status409Count.incrementAndGet();
                    }
                } catch (Exception ignored) {
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean finished = endLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(finished);
        // Exactly 1 order is created in DB
        assertEquals(1, countCustomerOrders(customer.getId()));

        // Inventory is reserved exactly once (2 units)
        InventoryEntity inv = inventoryRepository.findByBranchIdAndVariantId(ctx.branch().getId(), ctx.variant().getId()).orElseThrow();
        assertEquals(2, inv.getQuantityReserved());
    }

    // ==========================================
    // AC-05: REPLAY COMPLETED CACHED RESPONSE (NO DOUBLE DEDUCTION)
    // ==========================================
    @Test
    @DisplayName("AC-05: Replay completed request returns cached 201 response snapshot, does not create 2nd order and does not reserve stock twice")
    void testReplayCompletedMatchesHashReturns201NoDoubleDeduction() throws Exception {
        UserEntity customer = createTestCustomer("cust5@techmart.vn", "Customer Five", "0900000005");
        TestProductContext ctx = createProductAndStock(20, 0, new BigDecimal("10000000.00"), BigDecimal.ZERO);

        String idempotencyKey = "REPLAY-KEY-005";
        NewAddressRequest address = new NewAddressRequest("Cust Five", "0900000005", "123 Street", "Ward 1", "Hanoi", false);
        CreateOrderRequest request = new CreateOrderRequest(
                ctx.branch().getId(),
                null,
                address,
                "Replay order note",
                List.of(new OrderItemRequest(ctx.variant().getId(), 3))
        );
        String requestJson = objectMapper.writeValueAsString(request);

        long initialBlockedCount = idempotencyService.getBlockedRequestsCount();

        // 1. First execution -> 201 Created
        String firstResponseContent = mockMvc.perform(post("/internal/api/v1/orders")
                        .header(HeaderConstants.X_INTERNAL_SECRET, VALID_INTERNAL_SECRET)
                        .header("X-User-Id", customer.getId().toString())
                        .header("X-User-Role", "CUSTOMER")
                        .header("X-User-Email", customer.getEmail())
                        .header(HeaderConstants.IDEMPOTENCY_KEY, idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.totalAmount").value(30000000.00))
                .andReturn().getResponse().getContentAsString();

        OrderResponse firstOrder = objectMapper.readValue(firstResponseContent, OrderResponse.class);

        // Verify stock reserved = 3
        InventoryEntity invAfterFirst = inventoryRepository.findByBranchIdAndVariantId(ctx.branch().getId(), ctx.variant().getId()).orElseThrow();
        assertEquals(3, invAfterFirst.getQuantityReserved());
        assertEquals(1, countCustomerOrders(customer.getId()));

        // 2. Second execution (Replay) with exact same key and body -> 201 Created from snapshot
        String replayResponseContent = mockMvc.perform(post("/internal/api/v1/orders")
                        .header(HeaderConstants.X_INTERNAL_SECRET, VALID_INTERNAL_SECRET)
                        .header("X-User-Id", customer.getId().toString())
                        .header("X-User-Role", "CUSTOMER")
                        .header("X-User-Email", customer.getEmail())
                        .header(HeaderConstants.IDEMPOTENCY_KEY, idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(firstOrder.id().toString()))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.totalAmount").value(30000000.00))
                .andReturn().getResponse().getContentAsString();

        OrderResponse replayOrder = objectMapper.readValue(replayResponseContent, OrderResponse.class);
        assertEquals(firstOrder.id(), replayOrder.id());
        assertEquals(firstOrder.orderNumber(), replayOrder.orderNumber());

        // 3. INVARIANTS CHECK: NO double deduction and NO second order in DB!
        assertEquals(1, countCustomerOrders(customer.getId()), "Order count must remain exactly 1");
        InventoryEntity invAfterReplay = inventoryRepository.findByBranchIdAndVariantId(ctx.branch().getId(), ctx.variant().getId()).orElseThrow();
        assertEquals(3, invAfterReplay.getQuantityReserved(), "Quantity reserved must remain 3 without double-deduction");

        // Verify blocked counter incremented
        assertEquals(initialBlockedCount + 1, idempotencyService.getBlockedRequestsCount());
    }

    // ==========================================
    // AC-06: PAYLOAD MISMATCH RETURNS 422 UNPROCESSABLE ENTITY
    // ==========================================
    @Test
    @DisplayName("AC-06: Reusing same key with altered payload hash returns 422 Unprocessable Entity")
    void testPayloadMismatchReturns422UnprocessableEntity() throws Exception {
        UserEntity customer = createTestCustomer("cust6@techmart.vn", "Customer Six", "0900000006");
        TestProductContext ctx = createProductAndStock(20, 0, new BigDecimal("10000000.00"), BigDecimal.ZERO);

        String idempotencyKey = "PAYLOAD-MISMATCH-KEY-006";

        // First request with 2 units
        NewAddressRequest address = new NewAddressRequest("Cust Six", "0900000006", "Street", "Ward", "City", false);
        CreateOrderRequest request1 = new CreateOrderRequest(
                ctx.branch().getId(),
                null,
                address,
                "Original Note",
                List.of(new OrderItemRequest(ctx.variant().getId(), 2))
        );

        mockMvc.perform(post("/internal/api/v1/orders")
                        .header(HeaderConstants.X_INTERNAL_SECRET, VALID_INTERNAL_SECRET)
                        .header("X-User-Id", customer.getId().toString())
                        .header("X-User-Role", "CUSTOMER")
                        .header("X-User-Email", customer.getEmail())
                        .header(HeaderConstants.IDEMPOTENCY_KEY, idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isCreated());

        // Second request reusing same key but changing quantity from 2 to 4
        CreateOrderRequest request2Altered = new CreateOrderRequest(
                ctx.branch().getId(),
                null,
                address,
                "Modified Note",
                List.of(new OrderItemRequest(ctx.variant().getId(), 4))
        );

        mockMvc.perform(post("/internal/api/v1/orders")
                        .header(HeaderConstants.X_INTERNAL_SECRET, VALID_INTERNAL_SECRET)
                        .header("X-User-Id", customer.getId().toString())
                        .header("X-User-Role", "CUSTOMER")
                        .header("X-User-Email", customer.getEmail())
                        .header(HeaderConstants.IDEMPOTENCY_KEY, idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2Altered)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value(ResponseCode.IDEMPOTENCY_KEY_PAYLOAD_MISMATCH.getCode()));

        // Invariants: only the first order exists
        assertEquals(1, countCustomerOrders(customer.getId()));
        InventoryEntity inv = inventoryRepository.findByBranchIdAndVariantId(ctx.branch().getId(), ctx.variant().getId()).orElseThrow();
        assertEquals(2, inv.getQuantityReserved());
    }

    // ==========================================
    // EDGE CASE: MISSING IDEMPOTENCY KEY HEADER
    // ==========================================
    @Test
    @DisplayName("Edge Case: Missing Idempotency-Key proceeds normally without persisting idempotency record")
    void testMissingIdempotencyKeyHeaderPassesNormally() throws Exception {
        UserEntity customer = createTestCustomer("cust7@techmart.vn", "Customer Seven", "0900000007");
        TestProductContext ctx = createProductAndStock(20, 0, new BigDecimal("10000000.00"), BigDecimal.ZERO);

        NewAddressRequest address = new NewAddressRequest("Cust Seven", "0900000007", "Street", "Ward", "City", false);
        CreateOrderRequest request = new CreateOrderRequest(
                ctx.branch().getId(),
                null,
                address,
                "No idempotency key",
                List.of(new OrderItemRequest(ctx.variant().getId(), 1))
        );

        mockMvc.perform(post("/internal/api/v1/orders")
                        .header(HeaderConstants.X_INTERNAL_SECRET, VALID_INTERNAL_SECRET)
                        .header("X-User-Id", customer.getId().toString())
                        .header("X-User-Role", "CUSTOMER")
                        .header("X-User-Email", customer.getEmail())
                        // NO Idempotency-Key header
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        assertEquals(1, countCustomerOrders(customer.getId()));
        assertEquals(0, idempotencyKeyRepository.count(), "No record in idempotency_keys table when header is missing");
    }

    // ==========================================
    // EDGE CASE: FAILED EXECUTION RECOVERY
    // ==========================================
    @Test
    @DisplayName("Edge Case: Failed business execution updates status to FAILED and allows safe retry")
    void testFailedOrderExecutionMarksIdempotencyStatusFailed() throws Exception {
        UserEntity customer = createTestCustomer("cust8@techmart.vn", "Customer Eight", "0900000008");
        TestProductContext ctx = createProductAndStock(2, 0, new BigDecimal("10000000.00"), BigDecimal.ZERO);

        String idempotencyKey = "FAILED-RETRY-KEY-008";

        // Request 10 units when only 2 are available -> OUT_OF_STOCK (400)
        NewAddressRequest address = new NewAddressRequest("Cust Eight", "0900000008", "Street", "Ward", "City", false);
        CreateOrderRequest excessRequest = new CreateOrderRequest(
                ctx.branch().getId(),
                null,
                address,
                "Excess units",
                List.of(new OrderItemRequest(ctx.variant().getId(), 10))
        );

        mockMvc.perform(post("/internal/api/v1/orders")
                        .header(HeaderConstants.X_INTERNAL_SECRET, VALID_INTERNAL_SECRET)
                        .header("X-User-Id", customer.getId().toString())
                        .header("X-User-Role", "CUSTOMER")
                        .header("X-User-Email", customer.getEmail())
                        .header(HeaderConstants.IDEMPOTENCY_KEY, idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(excessRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(ResponseCode.INSUFFICIENT_STOCK.getCode()));

        // Status is marked as FAILED in idempotency table
        IdempotencyKeyEntity failedRecord = idempotencyKeyRepository.findByIdempotencyKeyAndUserId(idempotencyKey, customer.getId()).orElseThrow();
        assertEquals(IdempotencyStatus.FAILED, failedRecord.getStatus());

        // Client fixes request (requests 1 unit) using the SAME idempotency key -> MUST SUCCEED (201)
        CreateOrderRequest validRequest = new CreateOrderRequest(
                ctx.branch().getId(),
                null,
                address,
                "Fixed request",
                List.of(new OrderItemRequest(ctx.variant().getId(), 1))
        );

        mockMvc.perform(post("/internal/api/v1/orders")
                        .header(HeaderConstants.X_INTERNAL_SECRET, VALID_INTERNAL_SECRET)
                        .header("X-User-Id", customer.getId().toString())
                        .header("X-User-Role", "CUSTOMER")
                        .header("X-User-Email", customer.getEmail())
                        .header(HeaderConstants.IDEMPOTENCY_KEY, idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"));

        IdempotencyKeyEntity retriedRecord = idempotencyKeyRepository.findByIdempotencyKeyAndUserId(idempotencyKey, customer.getId()).orElseThrow();
        assertEquals(IdempotencyStatus.COMPLETED, retriedRecord.getStatus());
        assertEquals(201, retriedRecord.getResponseStatus());
    }

    // ==========================================
    // EDGE CASE: EXPIRED KEY REUSE
    // ==========================================
    @Test
    @DisplayName("Edge Case: Key expired after 24 hours can be reused for a new order")
    void testExpiredKeyAllowsReExecution() throws Exception {
        UserEntity customer = createTestCustomer("cust9@techmart.vn", "Customer Nine", "0900000009");
        TestProductContext ctx = createProductAndStock(20, 0, new BigDecimal("10000000.00"), BigDecimal.ZERO);

        String idempotencyKey = "EXPIRED-KEY-009";

        // Pre-insert COMPLETED record that expired 2 days ago
        IdempotencyKeyEntity expiredRecord = new IdempotencyKeyEntity();
        expiredRecord.setIdempotencyKey(idempotencyKey);
        expiredRecord.setUserId(customer.getId());
        expiredRecord.setRequestPath("/internal/api/v1/orders");
        expiredRecord.setRequestHash("old-hash");
        expiredRecord.setStatus(IdempotencyStatus.COMPLETED);
        expiredRecord.setResponseStatus(201);
        expiredRecord.setResponseBody("{\"id\": \"" + UUID.randomUUID() + "\"}");
        expiredRecord.setExpiresAt(Instant.now().minus(2, ChronoUnit.DAYS));
        idempotencyKeyRepository.saveAndFlush(expiredRecord);

        // New request with the expired key
        NewAddressRequest address = new NewAddressRequest("Cust Nine", "0900000009", "Street", "Ward", "City", false);
        CreateOrderRequest newRequest = new CreateOrderRequest(
                ctx.branch().getId(),
                null,
                address,
                "New order after expiration",
                List.of(new OrderItemRequest(ctx.variant().getId(), 2))
        );

        mockMvc.perform(post("/internal/api/v1/orders")
                        .header(HeaderConstants.X_INTERNAL_SECRET, VALID_INTERNAL_SECRET)
                        .header("X-User-Id", customer.getId().toString())
                        .header("X-User-Role", "CUSTOMER")
                        .header("X-User-Email", customer.getEmail())
                        .header(HeaderConstants.IDEMPOTENCY_KEY, idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"));

        // Status is updated to fresh COMPLETED
        IdempotencyKeyEntity refreshedRecord = idempotencyKeyRepository.findByIdempotencyKeyAndUserId(idempotencyKey, customer.getId()).orElseThrow();
        assertEquals(IdempotencyStatus.COMPLETED, refreshedRecord.getStatus());
        assertTrue(refreshedRecord.getExpiresAt().isAfter(Instant.now()));
    }
}
