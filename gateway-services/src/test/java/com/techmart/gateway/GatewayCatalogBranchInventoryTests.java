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
class GatewayCatalogBranchInventoryTests {

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
    // 1. PUBLIC ENDPOINTS (No token required)
    // ==========================================

    @Test
    @DisplayName("Gateway Public: Should permit GET /api/v1/categories/tree without token")
    void testPublicCategoryTreeWithoutToken() throws Exception {
        when(coreClient.forward(eq(HttpMethod.GET), eq("/internal/api/v1/categories/tree"), any(), eq(Object.class)))
                .thenReturn(ResponseEntity.ok("ok"));

        mockMvc.perform(get("/api/v1/categories/tree"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Gateway Public: Should permit GET /api/v1/brands without token")
    void testPublicBrandsWithoutToken() throws Exception {
        when(coreClient.forward(eq(HttpMethod.GET), eq("/internal/api/v1/brands"), any(), eq(Object.class)))
                .thenReturn(ResponseEntity.ok("ok"));

        mockMvc.perform(get("/api/v1/brands"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Gateway Public: Should permit GET /api/v1/branches without token")
    void testPublicBranchesWithoutToken() throws Exception {
        when(coreClient.forward(eq(HttpMethod.GET), eq("/internal/api/v1/branches"), any(), eq(Object.class)))
                .thenReturn(ResponseEntity.ok("ok"));

        mockMvc.perform(get("/api/v1/branches"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Gateway Public: Should permit GET /api/v1/products and detail endpoints without token")
    void testPublicProductsEndpointsWithoutToken() throws Exception {
        UUID prodId = UUID.randomUUID();

        when(coreClient.forward(eq(HttpMethod.GET), eq("/internal/api/v1/products"), any(), eq(Object.class)))
                .thenReturn(ResponseEntity.ok("ok"));
        when(coreClient.forward(eq(HttpMethod.GET), eq("/internal/api/v1/products/" + prodId), any(), eq(Object.class)))
                .thenReturn(ResponseEntity.ok("ok"));
        when(coreClient.forward(eq(HttpMethod.GET), eq("/internal/api/v1/products/slug/iphone-15-pro"), any(), eq(Object.class)))
                .thenReturn(ResponseEntity.ok("ok"));

        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/products/" + prodId))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/products/slug/iphone-15-pro"))
                .andExpect(status().isOk());
    }

    // ==========================================
    // 2. CATEGORY MANAGEMENT (ADMIN only)
    // ==========================================

    @Test
    @DisplayName("Gateway Security: Category modification endpoints must require ADMIN role")
    void testCategoryManagementSecurity() throws Exception {
        UUID catId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        // 1. Unauthenticated -> 401
        mockMvc.perform(post("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        // 2. CUSTOMER role -> 403
        String customerToken = jwtTokenProvider.generateAccessToken(userId, "cust@techmart.vn", "CUSTOMER");
        mockMvc.perform(post("/api/v1/categories")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        // 3. STAFF role -> 403
        String staffToken = jwtTokenProvider.generateAccessToken(userId, "staff@techmart.vn", "STAFF");
        mockMvc.perform(put("/api/v1/categories/" + catId)
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        // 4. ADMIN role -> permitted
        String adminToken = jwtTokenProvider.generateAccessToken(userId, "admin@techmart.vn", "ADMIN");
        when(coreClient.forward(eq(HttpMethod.POST), eq("/internal/api/v1/categories"), any(), eq(Object.class)))
                .thenReturn(ResponseEntity.ok("ok"));
        when(coreClient.forward(eq(HttpMethod.PUT), eq("/internal/api/v1/categories/" + catId), any(), eq(Object.class)))
                .thenReturn(ResponseEntity.ok("ok"));
        when(coreClient.forward(eq(HttpMethod.DELETE), eq("/internal/api/v1/categories/" + catId), any(), eq(Object.class)))
                .thenReturn(ResponseEntity.noContent().build());

        mockMvc.perform(post("/api/v1/categories")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/v1/categories/" + catId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/v1/categories/" + catId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());
    }

    // ==========================================
    // 3. BRAND & BRANCH CREATION (ADMIN only)
    // ==========================================

    @Test
    @DisplayName("Gateway Security: Brand and Branch creation must require ADMIN role")
    void testBrandAndBranchCreationSecurity() throws Exception {
        UUID userId = UUID.randomUUID();
        String staffToken = jwtTokenProvider.generateAccessToken(userId, "staff@techmart.vn", "STAFF");
        String adminToken = jwtTokenProvider.generateAccessToken(userId, "admin@techmart.vn", "ADMIN");

        // Brand: STAFF -> 403; ADMIN -> 200
        mockMvc.perform(post("/api/v1/brands")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        when(coreClient.forward(eq(HttpMethod.POST), eq("/internal/api/v1/brands"), any(), eq(Object.class)))
                .thenReturn(ResponseEntity.ok("ok"));

        mockMvc.perform(post("/api/v1/brands")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());

        // Branch: STAFF -> 403; ADMIN -> 200
        mockMvc.perform(post("/api/v1/branches")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        when(coreClient.forward(eq(HttpMethod.POST), eq("/internal/api/v1/branches"), any(), eq(Object.class)))
                .thenReturn(ResponseEntity.ok("ok"));

        mockMvc.perform(post("/api/v1/branches")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());
    }

    // ==========================================
    // 4. BRANCH INVENTORY (STAFF and ADMIN only)
    // ==========================================

    @Test
    @DisplayName("Gateway Security: Branch inventory inspection must be restricted to STAFF and ADMIN")
    void testBranchInventorySecurity() throws Exception {
        UUID branchId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        // 1. Unauthenticated -> 401
        mockMvc.perform(get("/api/v1/branches/" + branchId + "/inventory"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        // 2. CUSTOMER -> 403
        String customerToken = jwtTokenProvider.generateAccessToken(userId, "cust@techmart.vn", "CUSTOMER");
        mockMvc.perform(get("/api/v1/branches/" + branchId + "/inventory")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        // 3. STAFF -> 200 OK
        String staffToken = jwtTokenProvider.generateAccessToken(userId, "staff@techmart.vn", "STAFF");
        when(coreClient.forward(eq(HttpMethod.GET), eq("/internal/api/v1/branches/" + branchId + "/inventory"), any(), eq(Object.class)))
                .thenReturn(ResponseEntity.ok("ok"));

        mockMvc.perform(get("/api/v1/branches/" + branchId + "/inventory")
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk());

        // 4. ADMIN -> 200 OK
        String adminToken = jwtTokenProvider.generateAccessToken(userId, "admin@techmart.vn", "ADMIN");
        mockMvc.perform(get("/api/v1/branches/" + branchId + "/inventory")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    // ==========================================
    // 5. PRODUCT & VARIANT CREATION (STAFF and ADMIN)
    // ==========================================

    @Test
    @DisplayName("Gateway Security: Product & variant creation must allow both STAFF and ADMIN")
    void testProductCreationSecurity() throws Exception {
        UUID prodId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        // 1. Unauthenticated -> 401
        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        // 2. CUSTOMER -> 403
        String customerToken = jwtTokenProvider.generateAccessToken(userId, "cust@techmart.vn", "CUSTOMER");
        mockMvc.perform(post("/api/v1/products")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        // 3. STAFF -> 200 OK
        String staffToken = jwtTokenProvider.generateAccessToken(userId, "staff@techmart.vn", "STAFF");
        when(coreClient.forward(eq(HttpMethod.POST), eq("/internal/api/v1/products"), any(), eq(Object.class)))
                .thenReturn(ResponseEntity.ok("ok"));
        when(coreClient.forward(eq(HttpMethod.POST), eq("/internal/api/v1/products/" + prodId + "/variants"), any(), eq(Object.class)))
                .thenReturn(ResponseEntity.ok("ok"));

        mockMvc.perform(post("/api/v1/products")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/products/" + prodId + "/variants")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());
    }

    // ==========================================
    // 6. INVENTORY ADJUSTMENT (STAFF and ADMIN)
    // ==========================================

    @Test
    @DisplayName("Gateway Security: Inventory adjust must be restricted to STAFF and ADMIN")
    void testInventoryAdjustSecurity() throws Exception {
        UUID userId = UUID.randomUUID();

        // 1. Unauthenticated -> 401
        mockMvc.perform(post("/api/v1/inventory/adjust")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        // 2. CUSTOMER -> 403
        String customerToken = jwtTokenProvider.generateAccessToken(userId, "cust@techmart.vn", "CUSTOMER");
        mockMvc.perform(post("/api/v1/inventory/adjust")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        // 3. STAFF -> 200 OK
        String staffToken = jwtTokenProvider.generateAccessToken(userId, "staff@techmart.vn", "STAFF");
        when(coreClient.forward(eq(HttpMethod.POST), eq("/internal/api/v1/inventory/adjust"), any(), eq(Object.class)))
                .thenReturn(ResponseEntity.ok("ok"));

        mockMvc.perform(post("/api/v1/inventory/adjust")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());

        // 4. ADMIN -> 200 OK
        String adminToken = jwtTokenProvider.generateAccessToken(userId, "admin@techmart.vn", "ADMIN");
        mockMvc.perform(post("/api/v1/inventory/adjust")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());
    }
}
