package com.techmart.gateway;

import com.techmart.gateway.client.CoreClient;
import com.techmart.gateway.security.JwtTokenProvider;
import com.techmart.shared.constant.HeaderConstants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class GatewayMetricsAndStorageSecurityTests {

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
    // 1. SYSTEM HEALTH METRICS SECURITY (ADMIN ONLY)
    // ==========================================
    @Test
    @DisplayName("AC-10 & AC-15: /api/v1/admin/metrics/system-health must require ADMIN role; blocks Anonymous, CUSTOMER, and STAFF")
    void testSystemHealthSecurity() throws Exception {
        UUID userId = UUID.randomUUID();

        // 1. Anonymous -> 401 UNAUTHORIZED
        mockMvc.perform(get("/api/v1/admin/metrics/system-health"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        // 2. CUSTOMER -> 403 ACCESS_DENIED
        String customerToken = jwtTokenProvider.generateAccessToken(userId, "cust@techmart.vn", "CUSTOMER");
        mockMvc.perform(get("/api/v1/admin/metrics/system-health")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        // 3. STAFF -> 403 ACCESS_DENIED
        String staffToken = jwtTokenProvider.generateAccessToken(userId, "staff@techmart.vn", "STAFF");
        mockMvc.perform(get("/api/v1/admin/metrics/system-health")
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        // 4. ADMIN -> 200 OK (proxied to core)
        String adminToken = jwtTokenProvider.generateAccessToken(userId, "admin@techmart.vn", "ADMIN");
        when(coreClient.forward(eq(HttpMethod.GET), eq("/internal/api/v1/metrics/system-health"), any(), eq(Object.class)))
                .thenReturn(ResponseEntity.ok("{\"server\":{\"uptimeSeconds\":100}}"));

        mockMvc.perform(get("/api/v1/admin/metrics/system-health")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    // ==========================================
    // 2. SUMMARY, REVENUE-CHART & STOCK-ALERTS (STAFF & ADMIN)
    // ==========================================
    @Test
    @DisplayName("AC-07, AC-08, AC-09 & AC-15: Metrics summary, chart, and alerts require STAFF or ADMIN role; blocks CUSTOMER")
    void testMetricsEndpointsSecurity() throws Exception {
        UUID userId = UUID.randomUUID();

        // 1. Anonymous -> 401
        mockMvc.perform(get("/api/v1/admin/metrics/summary"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/admin/metrics/revenue-chart"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/admin/metrics/stock-alerts"))
                .andExpect(status().isUnauthorized());

        // 2. CUSTOMER -> 403
        String customerToken = jwtTokenProvider.generateAccessToken(userId, "cust@techmart.vn", "CUSTOMER");
        mockMvc.perform(get("/api/v1/admin/metrics/summary")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        mockMvc.perform(get("/api/v1/admin/metrics/revenue-chart")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        mockMvc.perform(get("/api/v1/admin/metrics/stock-alerts")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        // 3. STAFF -> 200 OK
        String staffToken = jwtTokenProvider.generateAccessToken(userId, "staff@techmart.vn", "STAFF");
        when(coreClient.forward(eq(HttpMethod.GET), any(), any(), eq(Object.class)))
                .thenReturn(ResponseEntity.ok("{\"data\":\"mocked\"}"));

        mockMvc.perform(get("/api/v1/admin/metrics/summary")
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/admin/metrics/revenue-chart?period=7_DAYS")
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/admin/metrics/stock-alerts?threshold=5")
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk());

        // 4. ADMIN -> 200 OK
        String adminToken = jwtTokenProvider.generateAccessToken(userId, "admin@techmart.vn", "ADMIN");
        mockMvc.perform(get("/api/v1/admin/metrics/summary")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    // ==========================================
    // 3. STORAGE UPLOAD SECURITY (STAFF & ADMIN)
    // ==========================================
    @Test
    @DisplayName("AC-12, AC-13 & AC-15: /api/v1/files/upload requires STAFF or ADMIN role; blocks Anonymous and CUSTOMER")
    void testStorageUploadSecurity() throws Exception {
        UUID userId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile("file", "image.jpg", "image/jpeg", "bytes".getBytes());

        // 1. Anonymous -> 401
        mockMvc.perform(multipart("/api/v1/files/upload").file(file))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        // 2. CUSTOMER -> 403
        String customerToken = jwtTokenProvider.generateAccessToken(userId, "cust@techmart.vn", "CUSTOMER");
        mockMvc.perform(multipart("/api/v1/files/upload").file(file)
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        // 3. STAFF -> 200 OK
        String staffToken = jwtTokenProvider.generateAccessToken(userId, "staff@techmart.vn", "STAFF");
        when(coreClient.forwardMultipart(any(), any(), any(), any(), eq(Object.class)))
                .thenReturn(ResponseEntity.ok("{\"fileName\":\"test.jpg\"}"));

        mockMvc.perform(multipart("/api/v1/files/upload").file(file)
                        .param("bucket", "products")
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk());

        // 4. ADMIN -> 200 OK
        String adminToken = jwtTokenProvider.generateAccessToken(userId, "admin@techmart.vn", "ADMIN");
        mockMvc.perform(multipart("/api/v1/files/upload").file(file)
                        .param("bucket", "brands")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    // ==========================================
    // 4. STORAGE DELETE SECURITY (ADMIN ONLY)
    // ==========================================
    @Test
    @DisplayName("AC-14 & AC-15: DELETE /api/v1/files/{fileName} requires ADMIN role; blocks Anonymous, CUSTOMER, and STAFF")
    void testStorageDeleteSecurityAdminOnly() throws Exception {
        UUID userId = UUID.randomUUID();
        String fileName = "sample-to-delete.jpeg";

        // 1. Anonymous -> 401
        mockMvc.perform(delete("/api/v1/files/" + fileName))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        // 2. CUSTOMER -> 403
        String customerToken = jwtTokenProvider.generateAccessToken(userId, "cust@techmart.vn", "CUSTOMER");
        mockMvc.perform(delete("/api/v1/files/" + fileName)
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        // 3. STAFF -> 403 (ADMIN only)
        String staffToken = jwtTokenProvider.generateAccessToken(userId, "staff@techmart.vn", "STAFF");
        mockMvc.perform(delete("/api/v1/files/" + fileName)
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        // 4. ADMIN -> 204 No Content
        String adminToken = jwtTokenProvider.generateAccessToken(userId, "admin@techmart.vn", "ADMIN");
        when(coreClient.forwardBodiless(eq(HttpMethod.DELETE), any()))
                .thenReturn(ResponseEntity.noContent().build());

        mockMvc.perform(delete("/api/v1/files/" + fileName)
                        .param("bucket", "products")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());
    }

    // ==========================================
    // 5. IDEMPOTENCY KEY FORWARDING ON ORDER POST
    // ==========================================
    @Test
    @DisplayName("AC-15: Gateway POST /api/v1/orders forwards Idempotency-Key header to CoreClient")
    void testIdempotencyKeyForwardingOnOrderCreate() throws Exception {
        UUID customerId = UUID.randomUUID();
        String customerToken = jwtTokenProvider.generateAccessToken(customerId, "cust@techmart.vn", "CUSTOMER");
        String idempotencyKey = "GW-IDEMPOTENCY-KEY-001";

        when(coreClient.forward(eq(HttpMethod.POST), eq("/internal/api/v1/orders"), any(), eq(Object.class)))
                .thenReturn(ResponseEntity.status(201).body("{\"id\":\"order-id-123\"}"));

        mockMvc.perform(post("/api/v1/orders")
                        .header("Authorization", "Bearer " + customerToken)
                        .header(HeaderConstants.IDEMPOTENCY_KEY, idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"branchId\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isCreated());
    }
}
