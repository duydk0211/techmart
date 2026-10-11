package com.techmart.gateway;

import com.techmart.gateway.client.CoreClient;
import com.techmart.gateway.security.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class GatewayCartOrderSecurityTests {

    static {
        System.setProperty("net.bytebuddy.experimental", "true");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CoreClient coreClient;

    // ==========================================
    // 1. CART SECURITY TESTS (/api/v1/cart/**)
    // ==========================================

    @Test
    @DisplayName("Gateway Cart Security: Cart endpoints must require CUSTOMER role")
    void testCartEndpointsSecurity() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();

        // 1. Unauthenticated -> 401 UNAUTHORIZED
        mockMvc.perform(get("/api/v1/cart"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        mockMvc.perform(post("/api/v1/cart/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"variantId\": \"" + UUID.randomUUID() + "\", \"quantity\": 1}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        mockMvc.perform(put("/api/v1/cart/items/" + itemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\": 2}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        mockMvc.perform(delete("/api/v1/cart/items/" + itemId))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        mockMvc.perform(delete("/api/v1/cart"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        // 2. STAFF role -> 403 ACCESS_DENIED
        String staffToken = jwtTokenProvider.generateAccessToken(userId, "staff@techmart.vn", "STAFF");

        mockMvc.perform(get("/api/v1/cart")
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        mockMvc.perform(post("/api/v1/cart/items")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"variantId\": \"" + UUID.randomUUID() + "\", \"quantity\": 1}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        // 3. ADMIN role -> 403 ACCESS_DENIED
        String adminToken = jwtTokenProvider.generateAccessToken(userId, "admin@techmart.vn", "ADMIN");

        mockMvc.perform(get("/api/v1/cart")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        // 4. CUSTOMER role -> 200 OK (Proxied successfully)
        String customerToken = jwtTokenProvider.generateAccessToken(userId, "cust@techmart.vn", "CUSTOMER");

        when(coreClient.forward(eq(HttpMethod.GET), eq("/internal/api/v1/cart"), any(), eq(Object.class)))
                .thenReturn(ResponseEntity.ok("cart_data"));
        when(coreClient.forward(eq(HttpMethod.POST), eq("/internal/api/v1/cart/items"), any(), eq(Object.class)))
                .thenReturn(ResponseEntity.ok("item_added"));
        when(coreClient.forward(eq(HttpMethod.PUT), eq("/internal/api/v1/cart/items/" + itemId), any(), eq(Object.class)))
                .thenReturn(ResponseEntity.ok("item_updated"));
        when(coreClient.forward(eq(HttpMethod.DELETE), eq("/internal/api/v1/cart/items/" + itemId), any(), eq(Void.class)))
                .thenReturn(ResponseEntity.noContent().build());
        when(coreClient.forward(eq(HttpMethod.DELETE), eq("/internal/api/v1/cart"), any(), eq(Void.class)))
                .thenReturn(ResponseEntity.noContent().build());

        mockMvc.perform(get("/api/v1/cart")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/cart/items")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"variantId\": \"" + UUID.randomUUID() + "\", \"quantity\": 1}"))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/v1/cart/items/" + itemId)
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\": 2}"))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/v1/cart/items/" + itemId)
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/v1/cart")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isNoContent());
    }

    // ==========================================
    // 2. ORDER SECURITY TESTS (/api/v1/orders/**)
    // ==========================================

    @Test
    @DisplayName("Gateway Order Security: POST /api/v1/orders permits CUSTOMER and STAFF, rejects unauthenticated")
    void testCreateOrderSecurity() throws Exception {
        UUID userId = UUID.randomUUID();

        // 1. Unauthenticated -> 401
        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        // 2. CUSTOMER role -> 200 OK
        String customerToken = jwtTokenProvider.generateAccessToken(userId, "cust@techmart.vn", "CUSTOMER");
        when(coreClient.forward(eq(HttpMethod.POST), eq("/internal/api/v1/orders"), any(), eq(Object.class)))
                .thenReturn(ResponseEntity.status(201).body("order_created"));

        mockMvc.perform(post("/api/v1/orders")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isCreated());

        // 3. STAFF role -> 201 Created
        String staffToken = jwtTokenProvider.generateAccessToken(userId, "staff@techmart.vn", "STAFF");
        mockMvc.perform(post("/api/v1/orders")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Gateway Order Security: GET /api/v1/orders/me requires CUSTOMER role")
    void testGetMyOrdersSecurity() throws Exception {
        UUID userId = UUID.randomUUID();

        // 1. Unauthenticated -> 401
        mockMvc.perform(get("/api/v1/orders/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        // 2. STAFF role -> 403 ACCESS_DENIED
        String staffToken = jwtTokenProvider.generateAccessToken(userId, "staff@techmart.vn", "STAFF");
        mockMvc.perform(get("/api/v1/orders/me")
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        // 3. CUSTOMER role -> 200 OK
        String customerToken = jwtTokenProvider.generateAccessToken(userId, "cust@techmart.vn", "CUSTOMER");
        when(coreClient.forward(eq(HttpMethod.GET), eq("/internal/api/v1/orders/me"), any(), eq(Object.class)))
                .thenReturn(ResponseEntity.ok("my_orders"));

        mockMvc.perform(get("/api/v1/orders/me")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Gateway Order Security: GET /api/v1/orders requires STAFF or ADMIN role, blocks CUSTOMER")
    void testGetAllOrdersSecurity() throws Exception {
        UUID userId = UUID.randomUUID();

        // 1. Unauthenticated -> 401
        mockMvc.perform(get("/api/v1/orders"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        // 2. CUSTOMER role -> 403 ACCESS_DENIED
        String customerToken = jwtTokenProvider.generateAccessToken(userId, "cust@techmart.vn", "CUSTOMER");
        mockMvc.perform(get("/api/v1/orders")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        // 3. STAFF role -> 200 OK
        String staffToken = jwtTokenProvider.generateAccessToken(userId, "staff@techmart.vn", "STAFF");
        when(coreClient.forward(eq(HttpMethod.GET), eq("/internal/api/v1/orders"), any(), eq(Object.class)))
                .thenReturn(ResponseEntity.ok("all_orders"));

        mockMvc.perform(get("/api/v1/orders")
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk());

        // 4. ADMIN role -> 200 OK
        String adminToken = jwtTokenProvider.generateAccessToken(userId, "admin@techmart.vn", "ADMIN");
        mockMvc.perform(get("/api/v1/orders")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Gateway Order Security: PUT /api/v1/orders/{id}/status requires STAFF or ADMIN role, blocks CUSTOMER")
    void testUpdateOrderStatusSecurity() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        // 1. Unauthenticated -> 401
        mockMvc.perform(put("/api/v1/orders/" + orderId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"CONFIRMED\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        // 2. CUSTOMER role -> 403 ACCESS_DENIED
        String customerToken = jwtTokenProvider.generateAccessToken(userId, "cust@techmart.vn", "CUSTOMER");
        mockMvc.perform(put("/api/v1/orders/" + orderId + "/status")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"CONFIRMED\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        // 3. STAFF role -> 200 OK
        String staffToken = jwtTokenProvider.generateAccessToken(userId, "staff@techmart.vn", "STAFF");
        when(coreClient.forward(eq(HttpMethod.PUT), eq("/internal/api/v1/orders/" + orderId + "/status"), any(), eq(Object.class)))
                .thenReturn(ResponseEntity.ok("status_updated"));

        mockMvc.perform(put("/api/v1/orders/" + orderId + "/status")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"CONFIRMED\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Gateway Order Security: POST /api/v1/orders/{id}/cancel requires authentication")
    void testCancelOrderSecurity() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        // 1. Unauthenticated -> 401
        mockMvc.perform(post("/api/v1/orders/" + orderId + "/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\": \"Changed mind\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        // 2. CUSTOMER role -> 200 OK (Allowed by Gateway, Core will check order status & ownership)
        String customerToken = jwtTokenProvider.generateAccessToken(userId, "cust@techmart.vn", "CUSTOMER");
        when(coreClient.forward(eq(HttpMethod.POST), eq("/internal/api/v1/orders/" + orderId + "/cancel"), any(), eq(Object.class)))
                .thenReturn(ResponseEntity.ok("order_cancelled"));

        mockMvc.perform(post("/api/v1/orders/" + orderId + "/cancel")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\": \"Changed mind\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Gateway Order Security: GET /api/v1/orders/{id} requires authentication")
    void testGetOrderDetailSecurity() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        // 1. Unauthenticated -> 401
        mockMvc.perform(get("/api/v1/orders/" + orderId))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        // 2. Authenticated user -> 200 OK (Core will check IDOR / ownership)
        String customerToken = jwtTokenProvider.generateAccessToken(userId, "cust@techmart.vn", "CUSTOMER");
        when(coreClient.forward(eq(HttpMethod.GET), eq("/internal/api/v1/orders/" + orderId), any(), eq(Object.class)))
                .thenReturn(ResponseEntity.ok("order_detail"));

        mockMvc.perform(get("/api/v1/orders/" + orderId)
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk());
    }
}
