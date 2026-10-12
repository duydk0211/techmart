package com.techmart.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.techmart.core.authorization.UserContext;
import com.techmart.core.dto.request.cart.AddToCartRequest;
import com.techmart.core.dto.request.cart.UpdateCartItemRequest;
import com.techmart.core.dto.request.order.CancelOrderRequest;
import com.techmart.core.dto.request.order.CreateOrderRequest;
import com.techmart.core.dto.request.order.NewAddressRequest;
import com.techmart.core.dto.request.order.OrderItemRequest;
import com.techmart.core.dto.request.order.UpdateOrderStatusRequest;
import com.techmart.core.dto.response.cart.CartItemResponse;
import com.techmart.core.dto.response.cart.CartResponse;
import com.techmart.core.dto.response.order.OrderItemResponse;
import com.techmart.core.dto.response.order.OrderResponse;
import com.techmart.core.dto.response.order.OrderSummaryResponse;
import com.techmart.core.entity.branch.BranchEntity;
import com.techmart.core.entity.cart.CartEntity;
import com.techmart.core.entity.cart.CartItemEntity;
import com.techmart.core.entity.catalog.BrandEntity;
import com.techmart.core.entity.catalog.CategoryEntity;
import com.techmart.core.entity.catalog.ProductEntity;
import com.techmart.core.entity.catalog.ProductVariantEntity;
import com.techmart.core.entity.inventory.InventoryEntity;
import com.techmart.core.entity.inventory.InventoryHistoryEntity;
import com.techmart.core.entity.order.OrderEntity;
import com.techmart.core.entity.order.OrderStatus;
import com.techmart.core.entity.user.AddressItem;
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
import com.techmart.core.repository.inventory.InventoryHistoryRepository;
import com.techmart.core.repository.inventory.InventoryRepository;
import com.techmart.core.repository.order.OrderItemRepository;
import com.techmart.core.repository.order.OrderRepository;
import com.techmart.core.repository.user.UserRepository;
import com.techmart.core.service.command.cart.CartCommandService;
import com.techmart.core.service.command.order.OrderCommandService;
import com.techmart.core.service.query.cart.CartQueryService;
import com.techmart.core.service.query.order.OrderQueryService;
import com.techmart.shared.constant.HeaderConstants;
import com.techmart.shared.constant.ResponseCode;
import com.techmart.shared.dto.PagedResponse;
import com.techmart.shared.exception.CoreException;
import org.junit.jupiter.api.AfterEach;
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
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class CartOrderConcurrencyCoreTests {

    private static final String VALID_INTERNAL_SECRET = "techmart_internal_secret_2026";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CartCommandService cartCommandService;

    @Autowired
    private CartQueryService cartQueryService;

    @Autowired
    private OrderCommandService orderCommandService;

    @Autowired
    private OrderQueryService orderQueryService;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private CategoryRepository categoryRepository;

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
        cleanUpDatabase();
    }

    @AfterEach
    void tearDown() {
        cleanUpDatabase();
    }

    private void cleanUpDatabase() {
        inventoryHistoryRepository.deleteAll();
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();
        cartItemRepository.deleteAll();
        cartRepository.deleteAll();
        inventoryRepository.deleteAll();
        productImageRepository.deleteAll();
        productVariantRepository.deleteAll();
        productRepository.deleteAll();
        branchRepository.deleteAll();
        brandRepository.deleteAll();

        categoryRepository.findAll().stream()
                .sorted((a, b) -> Integer.compare(b.getLevel(), a.getLevel()))
                .forEach(c -> categoryRepository.deleteById(c.getId()));

        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    // ==========================================
    // HELPER METHODS
    // ==========================================

    private UserEntity createTestCustomer(String email, String fullName, String phone) {
        UserEntity user = new UserEntity();
        user.setEmail(email);
        user.setFullName(fullName);
        user.setPasswordHash("$2a$10$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36WQoeG6Lruj3vjPGga31lW");
        user.setPhone(phone);
        user.setRole(Role.CUSTOMER);
        return userRepository.save(user);
    }

    private UserEntity createTestStaff(String email, String fullName) {
        UserEntity user = new UserEntity();
        user.setEmail(email);
        user.setFullName(fullName);
        user.setPasswordHash("$2a$10$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36WQoeG6Lruj3vjPGga31lW");
        user.setPhone("0901234567");
        user.setRole(Role.STAFF);
        return userRepository.save(user);
    }

    private TestProductContext createProductAndStock(int onHand, int reserved, BigDecimal price, BigDecimal discount) {
        CategoryEntity category = new CategoryEntity();
        category.setName("Laptops");
        category.setLevel(1);
        category.setPath("/laptops");
        category.setActive(true);
        category = categoryRepository.save(category);

        BrandEntity brand = new BrandEntity();
        brand.setName("Apple-" + UUID.randomUUID().toString().substring(0, 8));
        brand.setDescription("Apple Inc");
        brand.setActive(true);
        brand = brandRepository.save(brand);

        BranchEntity branch = new BranchEntity();
        branch.setName("TechMart HCM");
        branch.setAddress("123 Nguyen Hue, Q1, HCM");
        branch.setPhone("02812345678");
        branch.setActive(true);
        branch.setQuantityAvailable(onHand - reserved);
        branch = branchRepository.save(branch);

        ProductEntity product = new ProductEntity();
        product.setName("MacBook Pro M3 Max");
        product.setSlug("macbook-pro-m3-max-" + UUID.randomUUID().toString().substring(0, 8));
        product.setDescription("Powerful Apple silicon");
        product.setCategory(category);
        product.setBrand(brand);
        product.setStatus("ACTIVE");
        product = productRepository.save(product);

        ProductVariantEntity variant = new ProductVariantEntity();
        variant.setProduct(product);
        variant.setSku("MBP-M3MAX-" + UUID.randomUUID().toString().substring(0, 6));
        variant.setPrice(price);
        variant.setDiscountPercent(discount);
        variant.setStatus("ACTIVE");
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("ram", "36GB");
        attrs.put("ssd", "1TB");
        attrs.put("color", "Space Black");
        variant.setAttributes(attrs);
        variant = productVariantRepository.save(variant);

        InventoryEntity inventory = new InventoryEntity();
        inventory.setBranch(branch);
        inventory.setVariant(variant);
        inventory.setQuantityOnHand(onHand);
        inventory.setQuantityReserved(reserved);
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

    // ==========================================
    // 1. TC-01: DYNAMIC REAL-TIME PRICING & GET CART
    // ==========================================

    @Test
    @DisplayName("TC-01: Cart dynamic real-time pricing - Prices must reflect variant changes dynamically without altering cart_items")
    void testCartDynamicRealTimePricing() {
        UserEntity customer = createTestCustomer("customer@techmart.vn", "Customer One", "0900000001");
        TestProductContext ctx = createProductAndStock(10, 0, new BigDecimal("40000000.00"), new BigDecimal("10.00"));

        // 1. Add 2 units to cart (unitPrice 40M, 10% discount -> effective 36M each, subtotal 72M)
        CartResponse cartResponse = cartCommandService.addToCart(customer.getId(), new AddToCartRequest(ctx.variant().getId(), 2));
        assertEquals(2, cartResponse.totalItems());
        assertEquals(1, cartResponse.items().size());

        CartItemResponse item = cartResponse.items().get(0);
        assertEquals(new BigDecimal("40000000.00"), item.currentPrice());
        assertEquals(new BigDecimal("10.00"), item.discountPercent());
        assertEquals(new BigDecimal("36000000.00"), item.effectiveUnitPrice());
        assertEquals(new BigDecimal("72000000.00"), item.itemSubtotal());
        assertEquals(new BigDecimal("72000000.00"), cartResponse.estimatedTotalAmount());
        assertTrue(item.inStock());

        // 2. Change variant price in catalog to 50M with 20% discount (effective 40M)
        ProductVariantEntity variant = productVariantRepository.findById(ctx.variant().getId()).orElseThrow();
        variant.setPrice(new BigDecimal("50000000.00"));
        variant.setDiscountPercent(new BigDecimal("20.00"));
        productVariantRepository.save(variant);

        // 3. Query cart again - Dynamic calculation MUST show new effective price (40M each, subtotal 80M)
        CartResponse updatedCart = cartQueryService.getCart(customer.getId());
        assertEquals(2, updatedCart.totalItems());
        CartItemResponse updatedItem = updatedCart.items().get(0);
        assertEquals(new BigDecimal("50000000.00"), updatedItem.currentPrice());
        assertEquals(new BigDecimal("20.00"), updatedItem.discountPercent());
        assertEquals(new BigDecimal("40000000.00"), updatedItem.effectiveUnitPrice());
        assertEquals(new BigDecimal("80000000.00"), updatedItem.itemSubtotal());
        assertEquals(new BigDecimal("80000000.00"), updatedCart.estimatedTotalAmount());
        assertTrue(updatedItem.inStock());
    }

    // ==========================================
    // 2. TC-02: ADD TO CART & AUTO-ACCUMULATE QUANTITY
    // ==========================================

    @Test
    @DisplayName("TC-02: Add to cart - Must auto-accumulate quantity when adding same variant instead of duplicate items")
    void testAddToCartAutoAccumulateQuantity() {
        UserEntity customer = createTestCustomer("customer@techmart.vn", "Customer One", "0900000001");
        TestProductContext ctx = createProductAndStock(20, 0, new BigDecimal("10000000.00"), BigDecimal.ZERO);

        // Add 2 units
        cartCommandService.addToCart(customer.getId(), new AddToCartRequest(ctx.variant().getId(), 2));

        // Add 3 more units of the SAME variant
        CartResponse cart = cartCommandService.addToCart(customer.getId(), new AddToCartRequest(ctx.variant().getId(), 3));

        // Total quantity should be 5 in a single cart_item record
        assertEquals(5, cart.totalItems());
        assertEquals(1, cart.items().size());
        assertEquals(5, cart.items().get(0).quantity());

        // Verify DB records
        CartEntity cartEntity = cartRepository.findByCustomerId(customer.getId()).orElseThrow();
        List<CartItemEntity> items = cartItemRepository.findByCartIdWithVariantAndProduct(cartEntity.getId());
        assertEquals(1, items.size());
        assertEquals(5, items.get(0).getQuantity());
    }

    // ==========================================
    // 3. TC-03: UPDATE CART ITEM QUANTITY & VALIDATION
    // ==========================================

    @Test
    @DisplayName("TC-03: Update cart item quantity - Must reject quantity <= 0 and update valid quantity")
    void testUpdateCartItemQuantityValidation() {
        UserEntity customer = createTestCustomer("customer@techmart.vn", "Customer One", "0900000001");
        TestProductContext ctx = createProductAndStock(10, 0, new BigDecimal("5000000.00"), BigDecimal.ZERO);

        CartResponse initialCart = cartCommandService.addToCart(customer.getId(), new AddToCartRequest(ctx.variant().getId(), 1));
        UUID itemId = initialCart.items().get(0).id();

        // 1. Valid update to 4 units
        CartResponse updatedCart = cartCommandService.updateCartItem(customer.getId(), itemId, new UpdateCartItemRequest(4));
        assertEquals(4, updatedCart.totalItems());
        assertEquals(4, updatedCart.items().get(0).quantity());

        // 2. Reject quantity = 0 with VALIDATION_FAILED
        CoreException exZero = assertThrows(CoreException.class, () ->
                cartCommandService.updateCartItem(customer.getId(), itemId, new UpdateCartItemRequest(0)));
        assertEquals(ResponseCode.VALIDATION_FAILED, exZero.getResponseCode());

        // 3. Reject negative quantity with VALIDATION_FAILED
        CoreException exNeg = assertThrows(CoreException.class, () ->
                cartCommandService.updateCartItem(customer.getId(), itemId, new UpdateCartItemRequest(-2)));
        assertEquals(ResponseCode.VALIDATION_FAILED, exNeg.getResponseCode());
    }

    // ==========================================
    // 4. TC-04: REMOVE CART ITEM & ANTI-IDOR
    // ==========================================

    @Test
    @DisplayName("TC-04: Remove cart item - Owner can remove; other customers blocked with 403 FORBIDDEN_RESOURCE")
    void testRemoveCartItemAntiIdor() {
        UserEntity customerA = createTestCustomer("custA@techmart.vn", "Customer A", "0900000001");
        UserEntity customerB = createTestCustomer("custB@techmart.vn", "Customer B", "0900000002");
        TestProductContext ctx = createProductAndStock(10, 0, new BigDecimal("5000000.00"), BigDecimal.ZERO);

        CartResponse cartA = cartCommandService.addToCart(customerA.getId(), new AddToCartRequest(ctx.variant().getId(), 2));
        UUID itemAId = cartA.items().get(0).id();

        // Customer B attempts to remove Customer A's item -> MUST FAIL with FORBIDDEN_RESOURCE
        CoreException exIdor = assertThrows(CoreException.class, () ->
                cartCommandService.removeCartItem(customerB.getId(), itemAId));
        assertEquals(ResponseCode.FORBIDDEN_RESOURCE, exIdor.getResponseCode());

        // Customer A removes own item -> SUCCESS
        cartCommandService.removeCartItem(customerA.getId(), itemAId);
        CartResponse emptyCart = cartQueryService.getCart(customerA.getId());
        assertEquals(0, emptyCart.totalItems());
        assertTrue(emptyCart.items().isEmpty());

        // Clear cart test
        cartCommandService.addToCart(customerA.getId(), new AddToCartRequest(ctx.variant().getId(), 3));
        cartCommandService.clearCart(customerA.getId());
        CartResponse clearedCart = cartQueryService.getCart(customerA.getId());
        assertEquals(0, clearedCart.totalItems());
    }

    // ==========================================
    // 5. TC-05: HAPPY PATH ORDER CREATION & SNAPSHOT PATTERN
    // ==========================================

    @Test
    @DisplayName("TC-05: Order creation happy path - Snapshots shipping address and order_items, cleans cart, reserves inventory")
    void testCreateOrderHappyPathAndSnapshot() {
        UserEntity customer = createTestCustomer("cust@techmart.vn", "Nguyen Van An", "0987654321");
        TestProductContext ctx = createProductAndStock(15, 2, new BigDecimal("25000000.00"), new BigDecimal("10.00"));

        // Put item in cart first to verify cart cleanup
        cartCommandService.addToCart(customer.getId(), new AddToCartRequest(ctx.variant().getId(), 3));
        assertEquals(3, cartQueryService.getCart(customer.getId()).totalItems());

        NewAddressRequest address = new NewAddressRequest(
                "Nguyen Van An",
                "0987654321",
                "123 Vo Van Tan",
                "Phuong Vo Thi Sau",
                "Quan 3, TP.HCM",
                true
        );

        CreateOrderRequest orderReq = new CreateOrderRequest(
                ctx.branch().getId(),
                null,
                address,
                "Giao gio hanh chinh",
                List.of(new OrderItemRequest(ctx.variant().getId(), 3))
        );

        OrderResponse orderRes = orderCommandService.createOrder(customer.getId(), orderReq);

        // 1. Verify Order metadata
        assertNotNull(orderRes.id());
        assertNotNull(orderRes.orderNumber());
        assertTrue(orderRes.orderNumber().startsWith("ORD-"));
        assertEquals("PENDING", orderRes.status());
        assertEquals("123 Vo Van Tan, Phuong Vo Thi Sau, Quan 3, TP.HCM", orderRes.shippingAddress());
        assertEquals("Nguyen Van An", orderRes.customerName());
        assertEquals("0987654321", orderRes.customerPhone());
        assertEquals("cust@techmart.vn", orderRes.customerEmail());
        assertEquals("Giao gio hanh chinh", orderRes.note());

        // 2. Verify Snapshot calculations (25M * 0.9 = 22.5M each * 3 = 67.5M)
        BigDecimal expectedUnit = new BigDecimal("22500000.00");
        BigDecimal expectedTotal = new BigDecimal("67500000.00");
        assertEquals(expectedTotal, orderRes.totalAmount());
        assertEquals(1, orderRes.items().size());

        OrderItemResponse itemSnapshot = orderRes.items().get(0);
        assertEquals("MacBook Pro M3 Max", itemSnapshot.productName());
        assertEquals(ctx.variant().getSku(), itemSnapshot.variantSku());
        assertEquals(new BigDecimal("25000000.00"), itemSnapshot.unitPrice());
        assertEquals(new BigDecimal("10.00"), itemSnapshot.discountPercent());
        assertEquals(3, itemSnapshot.quantity());
        assertEquals(expectedTotal, itemSnapshot.subtotal());
        assertNotNull(itemSnapshot.variantAttributes());
        assertEquals("Space Black", itemSnapshot.variantAttributes().get("color"));

        // 3. Verify Inventory reservation
        InventoryEntity updatedInv = inventoryRepository.findByBranchIdAndVariantId(ctx.branch().getId(), ctx.variant().getId()).orElseThrow();
        assertEquals(15, updatedInv.getQuantityOnHand());
        assertEquals(5, updatedInv.getQuantityReserved(), "Initial 2 reserved + 3 new reserved = 5");

        // 4. Verify Inventory history log
        List<InventoryHistoryEntity> historyList = inventoryHistoryRepository.findAll();
        assertEquals(1, historyList.size());
        InventoryHistoryEntity history = historyList.get(0);
        assertEquals("RESERVE", history.getChangeType());
        assertEquals(3, history.getQuantityChange());
        assertEquals(2, history.getQuantityBefore());
        assertEquals(5, history.getQuantityAfter());

        // 5. Verify Cart Cleanup
        CartResponse cleanedCart = cartQueryService.getCart(customer.getId());
        assertEquals(0, cleanedCart.totalItems());
        assertTrue(cleanedCart.items().isEmpty());
    }

    // ==========================================
    // 6. TC-06: CONCURRENCY & ANTI-OVERSELLING TEST
    // ==========================================

    @Test
    @DisplayName("TC-06: Concurrency anti-overselling - 2 concurrent threads fighting for 1 last available item: exactly 1 wins, 1 receives 409 Conflict, stock never negative")
    void testConcurrentOrderAntiOverselling() throws InterruptedException {
        // Setup inventory with onHand = 1, reserved = 0 (exactly 1 item available)
        TestProductContext ctx = createProductAndStock(1, 0, new BigDecimal("30000000.00"), BigDecimal.ZERO);

        UserEntity user1 = createTestCustomer("buyer1@techmart.vn", "Buyer One", "0900000011");
        UserEntity user2 = createTestCustomer("buyer2@techmart.vn", "Buyer Two", "0900000022");

        NewAddressRequest addr1 = new NewAddressRequest("Buyer One", "0900000011", "Street 1", "Ward 1", "HCM", false);
        NewAddressRequest addr2 = new NewAddressRequest("Buyer Two", "0900000022", "Street 2", "Ward 2", "HCM", false);

        CreateOrderRequest req1 = new CreateOrderRequest(ctx.branch().getId(), null, addr1, "Order 1", List.of(new OrderItemRequest(ctx.variant().getId(), 1)));
        CreateOrderRequest req2 = new CreateOrderRequest(ctx.branch().getId(), null, addr2, "Order 2", List.of(new OrderItemRequest(ctx.variant().getId(), 1)));

        int threadCount = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);
        AtomicReference<Throwable> otherError = new AtomicReference<>();

        Runnable task1 = () -> {
            try {
                readyLatch.countDown();
                startLatch.await();
                orderCommandService.createOrder(user1.getId(), req1);
                successCount.incrementAndGet();
            } catch (CoreException e) {
                if (e.getResponseCode() == ResponseCode.INSUFFICIENT_STOCK) {
                    conflictCount.incrementAndGet();
                } else {
                    otherError.set(e);
                }
            } catch (Throwable t) {
                otherError.set(t);
            } finally {
                doneLatch.countDown();
            }
        };

        Runnable task2 = () -> {
            try {
                readyLatch.countDown();
                startLatch.await();
                orderCommandService.createOrder(user2.getId(), req2);
                successCount.incrementAndGet();
            } catch (CoreException e) {
                if (e.getResponseCode() == ResponseCode.INSUFFICIENT_STOCK) {
                    conflictCount.incrementAndGet();
                } else {
                    otherError.set(e);
                }
            } catch (Throwable t) {
                otherError.set(t);
            } finally {
                doneLatch.countDown();
            }
        };

        executor.submit(task1);
        executor.submit(task2);

        assertTrue(readyLatch.await(5, TimeUnit.SECONDS), "Threads must be ready");
        startLatch.countDown(); // Fire simultaneous race
        assertTrue(doneLatch.await(10, TimeUnit.SECONDS), "Threads must finish execution");
        executor.shutdown();

        assertNull(otherError.get(), "No unexpected error should occur during concurrency: " + otherError.get());

        // Invariant Validation: Exactly 1 thread succeeds, exactly 1 thread gets INSUFFICIENT_STOCK
        assertEquals(1, successCount.get(), "Exactly one buyer must successfully create order");
        assertEquals(1, conflictCount.get(), "Exactly one buyer must receive INSUFFICIENT_STOCK (409 Conflict)");

        // DB Invariant: Stock must NOT be oversold or negative
        InventoryEntity finalInv = inventoryRepository.findByBranchIdAndVariantId(ctx.branch().getId(), ctx.variant().getId()).orElseThrow();
        assertEquals(1, finalInv.getQuantityOnHand());
        assertEquals(1, finalInv.getQuantityReserved());
        int available = finalInv.getQuantityOnHand() - finalInv.getQuantityReserved();
        assertEquals(0, available, "Available stock must be 0, never negative!");
    }

    // ==========================================
    // 7. TC-07: DEADLOCK PREVENTION INVARIANT
    // ==========================================

    @Test
    @DisplayName("TC-07: Deadlock prevention invariant - Multi-item order must sort variantId ASC before executing atomic reserveStock")
    void testDeadlockPreventionSorting() {
        UserEntity customer = createTestCustomer("cust@techmart.vn", "Customer Deadlock", "0900000001");
        TestProductContext ctx = createProductAndStock(20, 0, new BigDecimal("10000000.00"), BigDecimal.ZERO);

        // Create a second variant on the same product
        ProductVariantEntity variant2 = new ProductVariantEntity();
        variant2.setProduct(ctx.product());
        variant2.setSku("MBP-M3MAX-SECOND-" + UUID.randomUUID().toString().substring(0, 6));
        variant2.setPrice(new BigDecimal("15000000.00"));
        variant2.setDiscountPercent(BigDecimal.ZERO);
        variant2.setStatus("ACTIVE");
        variant2.setAttributes(Map.of("color", "Silver"));
        variant2 = productVariantRepository.save(variant2);

        InventoryEntity inv2 = new InventoryEntity();
        inv2.setBranch(ctx.branch());
        inv2.setVariant(variant2);
        inv2.setQuantityOnHand(20);
        inv2.setQuantityReserved(0);
        inventoryRepository.save(inv2);

        UUID var1Id = ctx.variant().getId();
        UUID var2Id = variant2.getId();

        // Pass them in intentionally reverse order of UUID comparison
        UUID firstId = var1Id.compareTo(var2Id) > 0 ? var1Id : var2Id;
        UUID secondId = var1Id.compareTo(var2Id) > 0 ? var2Id : var1Id;

        List<OrderItemRequest> reverseOrderedItems = List.of(
                new OrderItemRequest(firstId, 2),
                new OrderItemRequest(secondId, 1)
        );

        NewAddressRequest address = new NewAddressRequest("Customer", "0900000001", "Street", "Ward", "City", false);
        CreateOrderRequest request = new CreateOrderRequest(ctx.branch().getId(), null, address, "Deadlock test", reverseOrderedItems);

        // System sorts them by variantId ASC internally without deadlocking
        OrderResponse orderRes = orderCommandService.createOrder(customer.getId(), request);
        assertNotNull(orderRes);
        assertEquals(2, orderRes.items().size());
    }

    // ==========================================
    // 8. TC-08: CUSTOMER ORDER HISTORY (GET /orders/me)
    // ==========================================

    @Test
    @DisplayName("TC-08: Order history - Customer can only view their own orders via /orders/me")
    void testGetMyOrdersIsolation() {
        UserEntity customerA = createTestCustomer("custA@techmart.vn", "Cust A", "0900000001");
        UserEntity customerB = createTestCustomer("custB@techmart.vn", "Cust B", "0900000002");
        TestProductContext ctx = createProductAndStock(20, 0, new BigDecimal("10000000.00"), BigDecimal.ZERO);

        NewAddressRequest addr = new NewAddressRequest("A", "0900000001", "Street", "Ward", "City", false);

        // Customer A creates 2 orders
        orderCommandService.createOrder(customerA.getId(), new CreateOrderRequest(
                ctx.branch().getId(), null, addr, "A-1", List.of(new OrderItemRequest(ctx.variant().getId(), 1))));
        orderCommandService.createOrder(customerA.getId(), new CreateOrderRequest(
                ctx.branch().getId(), null, addr, "A-2", List.of(new OrderItemRequest(ctx.variant().getId(), 2))));

        // Customer B creates 1 order
        orderCommandService.createOrder(customerB.getId(), new CreateOrderRequest(
                ctx.branch().getId(), null, addr, "B-1", List.of(new OrderItemRequest(ctx.variant().getId(), 1))));

        // Query Customer A orders
        PagedResponse<OrderSummaryResponse> aOrders = orderQueryService.getMyOrders(customerA.getId(), PageRequest.of(0, 10));
        assertEquals(2, aOrders.getTotalElements());
        assertEquals(2, aOrders.getContent().size());

        // Query Customer B orders
        PagedResponse<OrderSummaryResponse> bOrders = orderQueryService.getMyOrders(customerB.getId(), PageRequest.of(0, 10));
        assertEquals(1, bOrders.getTotalElements());
        assertEquals(1, bOrders.getContent().size());
    }

    // ==========================================
    // 9. TC-09: VIEW ORDER DETAIL & ANTI-IDOR
    // ==========================================

    @Test
    @DisplayName("TC-09: Order detail anti-IDOR - Owner and Staff can view; other customer blocked with 403 FORBIDDEN_RESOURCE")
    void testGetOrderDetailAntiIdor() {
        UserEntity customerA = createTestCustomer("custA@techmart.vn", "Cust A", "0900000001");
        UserEntity customerB = createTestCustomer("custB@techmart.vn", "Cust B", "0900000002");
        UserEntity staff = createTestStaff("staff@techmart.vn", "Staff One");
        TestProductContext ctx = createProductAndStock(20, 0, new BigDecimal("10000000.00"), BigDecimal.ZERO);

        NewAddressRequest addr = new NewAddressRequest("A", "0900000001", "Street", "Ward", "City", false);
        OrderResponse orderA = orderCommandService.createOrder(customerA.getId(), new CreateOrderRequest(
                ctx.branch().getId(), null, addr, "Order A", List.of(new OrderItemRequest(ctx.variant().getId(), 1))));

        // 1. Customer A views own order -> 200 OK
        OrderResponse viewedByOwner = orderQueryService.getOrderDetail(orderA.id(), customerA.getId(), "CUSTOMER");
        assertEquals(orderA.id(), viewedByOwner.id());

        // 2. Customer B attempts to view Customer A's order -> 403 FORBIDDEN_RESOURCE
        CoreException exIdor = assertThrows(CoreException.class, () ->
                orderQueryService.getOrderDetail(orderA.id(), customerB.getId(), "CUSTOMER"));
        assertEquals(ResponseCode.FORBIDDEN_RESOURCE, exIdor.getResponseCode());

        // 3. Staff views Customer A's order -> 200 OK
        OrderResponse viewedByStaff = orderQueryService.getOrderDetail(orderA.id(), staff.getId(), "STAFF");
        assertEquals(orderA.id(), viewedByStaff.id());
    }

    // ==========================================
    // 10. TC-10: SINGLE-DIRECTION STATE MACHINE
    // ==========================================

    @Test
    @DisplayName("TC-10: State Machine - Sequential PENDING -> CONFIRMED -> PROCESSING -> SHIPPED succeeds; illegal state skip rejected with 422")
    void testOrderStateMachineTransitions() {
        UserEntity customer = createTestCustomer("cust@techmart.vn", "Cust State", "0900000001");
        TestProductContext ctx = createProductAndStock(20, 0, new BigDecimal("10000000.00"), BigDecimal.ZERO);

        NewAddressRequest addr = new NewAddressRequest("Cust", "0900000001", "Street", "Ward", "City", false);
        OrderResponse order = orderCommandService.createOrder(customer.getId(), new CreateOrderRequest(
                ctx.branch().getId(), null, addr, "State Machine", List.of(new OrderItemRequest(ctx.variant().getId(), 2))));

        assertEquals("PENDING", order.status());

        // 1. Illegal jump from PENDING -> SHIPPED must throw INVALID_ORDER_STATE (422)
        CoreException exJump = assertThrows(CoreException.class, () ->
                orderCommandService.updateOrderStatus(order.id(), new UpdateOrderStatusRequest("SHIPPED", "Skipping steps")));
        assertEquals(ResponseCode.INVALID_ORDER_STATE, exJump.getResponseCode());

        // 2. Valid sequential transitions:
        // PENDING -> CONFIRMED
        OrderResponse res1 = orderCommandService.updateOrderStatus(order.id(), new UpdateOrderStatusRequest("CONFIRMED", "Order confirmed"));
        assertEquals("CONFIRMED", res1.status());

        // CONFIRMED -> PROCESSING
        OrderResponse res2 = orderCommandService.updateOrderStatus(order.id(), new UpdateOrderStatusRequest("PROCESSING", "Packing items"));
        assertEquals("PROCESSING", res2.status());

        // PROCESSING -> SHIPPED
        OrderResponse res3 = orderCommandService.updateOrderStatus(order.id(), new UpdateOrderStatusRequest("SHIPPED", "Shipped with VNPost"));
        assertEquals("SHIPPED", res3.status());
    }

    // ==========================================
    // 11. TC-11: DELIVERED & PERMANENT STOCK DEDUCTION
    // ==========================================

    @Test
    @DisplayName("TC-11: Delivery success - Transitions to DELIVERED, deducts physical stock and reserved units, updates branch available stock, logs SALE_DEDUCT")
    void testOrderDeliveredStockDeduction() {
        UserEntity customer = createTestCustomer("cust@techmart.vn", "Cust Delivered", "0900000001");
        TestProductContext ctx = createProductAndStock(20, 0, new BigDecimal("10000000.00"), BigDecimal.ZERO);

        NewAddressRequest addr = new NewAddressRequest("Cust", "0900000001", "Street", "Ward", "City", false);
        OrderResponse order = orderCommandService.createOrder(customer.getId(), new CreateOrderRequest(
                ctx.branch().getId(), null, addr, "Delivery test", List.of(new OrderItemRequest(ctx.variant().getId(), 4))));

        // Advance to SHIPPED
        orderCommandService.updateOrderStatus(order.id(), new UpdateOrderStatusRequest("CONFIRMED", null));
        orderCommandService.updateOrderStatus(order.id(), new UpdateOrderStatusRequest("PROCESSING", null));
        orderCommandService.updateOrderStatus(order.id(), new UpdateOrderStatusRequest("SHIPPED", null));

        // Advance to DELIVERED
        OrderResponse deliveredOrder = orderCommandService.updateOrderStatus(order.id(), new UpdateOrderStatusRequest("DELIVERED", "Handed to customer"));
        assertEquals("DELIVERED", deliveredOrder.status());

        // 1. Verify Inventory: on_hand was 20 -> 16, reserved was 4 -> 0
        InventoryEntity inv = inventoryRepository.findByBranchIdAndVariantId(ctx.branch().getId(), ctx.variant().getId()).orElseThrow();
        assertEquals(16, inv.getQuantityOnHand());
        assertEquals(0, inv.getQuantityReserved());

        // 2. Verify Branch available stock: was 20 -> 16
        BranchEntity branch = branchRepository.findById(ctx.branch().getId()).orElseThrow();
        assertEquals(16, branch.getQuantityAvailable());

        // 3. Verify SALE_DEDUCT history record
        List<InventoryHistoryEntity> histories = inventoryHistoryRepository.findAll().stream()
                .filter(h -> "SALE_DEDUCT".equals(h.getChangeType()))
                .toList();
        assertEquals(1, histories.size());
        InventoryHistoryEntity saleLog = histories.get(0);
        assertEquals(-4, saleLog.getQuantityChange());
        assertEquals(20, saleLog.getQuantityBefore());
        assertEquals(16, saleLog.getQuantityAfter());
    }

    // ==========================================
    // 12. TC-12: CANCEL ORDER WHEN PENDING & RELEASE STOCK
    // ==========================================

    @Test
    @DisplayName("TC-12: Cancel order when PENDING - Cancels order, releases reserved stock, logs RELEASE")
    void testCancelPendingOrderAndReleaseStock() {
        UserEntity customer = createTestCustomer("cust@techmart.vn", "Cust Cancel", "0900000001");
        TestProductContext ctx = createProductAndStock(10, 0, new BigDecimal("10000000.00"), BigDecimal.ZERO);

        NewAddressRequest addr = new NewAddressRequest("Cust", "0900000001", "Street", "Ward", "City", false);
        OrderResponse order = orderCommandService.createOrder(customer.getId(), new CreateOrderRequest(
                ctx.branch().getId(), null, addr, "Cancel test", List.of(new OrderItemRequest(ctx.variant().getId(), 3))));

        // Initially 3 items reserved
        InventoryEntity invBefore = inventoryRepository.findByBranchIdAndVariantId(ctx.branch().getId(), ctx.variant().getId()).orElseThrow();
        assertEquals(3, invBefore.getQuantityReserved());

        // Customer cancels order
        OrderResponse cancelledOrder = orderCommandService.cancelOrder(
                order.id(),
                new CancelOrderRequest("Changed my mind"),
                customer.getId(),
                "CUSTOMER"
        );

        assertEquals("CANCELLED", cancelledOrder.status());
        assertEquals("Changed my mind", cancelledOrder.cancelledReason());

        // 1. Verify reserved stock released back to 0, onHand remains 10
        InventoryEntity invAfter = inventoryRepository.findByBranchIdAndVariantId(ctx.branch().getId(), ctx.variant().getId()).orElseThrow();
        assertEquals(10, invAfter.getQuantityOnHand());
        assertEquals(0, invAfter.getQuantityReserved());

        // 2. Verify RELEASE history record
        List<InventoryHistoryEntity> histories = inventoryHistoryRepository.findAll().stream()
                .filter(h -> "RELEASE".equals(h.getChangeType()))
                .toList();
        assertEquals(1, histories.size());
        InventoryHistoryEntity releaseLog = histories.get(0);
        assertEquals(-3, releaseLog.getQuantityChange());
        assertEquals(3, releaseLog.getQuantityBefore());
        assertEquals(0, releaseLog.getQuantityAfter());
    }

    // ==========================================
    // 13. TC-13: BLOCK CANCELLATION OF SHIPPED ORDERS
    // ==========================================

    @Test
    @DisplayName("TC-13: Block cancellation of SHIPPED orders - Must reject cancellation with 422 ORDER_CANNOT_BE_CANCELLED")
    void testBlockCancelShippedOrder() {
        UserEntity customer = createTestCustomer("cust@techmart.vn", "Cust Shipped", "0900000001");
        UserEntity staff = createTestStaff("staff@techmart.vn", "Staff Shipped");
        TestProductContext ctx = createProductAndStock(10, 0, new BigDecimal("10000000.00"), BigDecimal.ZERO);

        NewAddressRequest addr = new NewAddressRequest("Cust", "0900000001", "Street", "Ward", "City", false);
        OrderResponse order = orderCommandService.createOrder(customer.getId(), new CreateOrderRequest(
                ctx.branch().getId(), null, addr, "Shipped block test", List.of(new OrderItemRequest(ctx.variant().getId(), 2))));

        // Advance order to SHIPPED
        orderCommandService.updateOrderStatus(order.id(), new UpdateOrderStatusRequest("CONFIRMED", null));
        orderCommandService.updateOrderStatus(order.id(), new UpdateOrderStatusRequest("PROCESSING", null));
        orderCommandService.updateOrderStatus(order.id(), new UpdateOrderStatusRequest("SHIPPED", null));

        // 1. Customer attempts to cancel SHIPPED order -> MUST FAIL with ORDER_CANNOT_BE_CANCELLED
        CoreException exCust = assertThrows(CoreException.class, () ->
                orderCommandService.cancelOrder(order.id(), new CancelOrderRequest("Customer cancel shipped"), customer.getId(), "CUSTOMER"));
        assertEquals(ResponseCode.ORDER_CANNOT_BE_CANCELLED, exCust.getResponseCode());

        // 2. Staff attempts to cancel SHIPPED order -> MUST FAIL with ORDER_CANNOT_BE_CANCELLED
        CoreException exStaff = assertThrows(CoreException.class, () ->
                orderCommandService.cancelOrder(order.id(), new CancelOrderRequest("Staff cancel shipped"), staff.getId(), "STAFF"));
        assertEquals(ResponseCode.ORDER_CANNOT_BE_CANCELLED, exStaff.getResponseCode());

        // 3. Verify order status remains SHIPPED and stock remains reserved
        OrderEntity dbOrder = orderRepository.findById(order.id()).orElseThrow();
        assertEquals(OrderStatus.SHIPPED, dbOrder.getStatus());
        InventoryEntity inv = inventoryRepository.findByBranchIdAndVariantId(ctx.branch().getId(), ctx.variant().getId()).orElseThrow();
        assertEquals(2, inv.getQuantityReserved());
    }

    // ==========================================
    // 14. TC-14: ZERO-TRUST GATEWAY -> CORE SERVICE SECURITY
    // ==========================================

    @Test
    @DisplayName("TC-14: Zero-Trust Security - Core service internal endpoints reject requests missing or having invalid X-Internal-Secret with 403")
    void testCoreInternalEndpointsZeroTrustSecurity() throws Exception {
        UUID randomId = UUID.randomUUID();

        // 1. Missing secret on GET /internal/api/v1/cart -> 403 INVALID_INTERNAL_SECRET
        mockMvc.perform(get("/internal/api/v1/cart"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ResponseCode.INVALID_INTERNAL_SECRET.getCode()))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.timestamp").value(notNullValue()));

        // 2. Wrong secret on POST /internal/api/v1/cart/items -> 403 INVALID_INTERNAL_SECRET
        mockMvc.perform(post("/internal/api/v1/cart/items")
                        .header(HeaderConstants.X_INTERNAL_SECRET, "wrong_secret_techmart")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ResponseCode.INVALID_INTERNAL_SECRET.getCode()));

        // 3. Missing secret on POST /internal/api/v1/orders -> 403 INVALID_INTERNAL_SECRET
        mockMvc.perform(post("/internal/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ResponseCode.INVALID_INTERNAL_SECRET.getCode()));

        // 4. Missing secret on GET /internal/api/v1/orders/me -> 403 INVALID_INTERNAL_SECRET
        mockMvc.perform(get("/internal/api/v1/orders/me"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ResponseCode.INVALID_INTERNAL_SECRET.getCode()));

        // 5. Missing secret on PUT /internal/api/v1/orders/{id}/status -> 403 INVALID_INTERNAL_SECRET
        mockMvc.perform(put("/internal/api/v1/orders/" + randomId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ResponseCode.INVALID_INTERNAL_SECRET.getCode()));
    }
}
