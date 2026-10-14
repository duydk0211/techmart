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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class GatewayBannerSecurityTests {

    static {
        System.setProperty("net.bytebuddy.experimental", "true");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CoreClient coreClient;

    @Test
    @DisplayName("GET /api/v1/banners is public and allows anonymous requests")
    void testGetBannersPublic() throws Exception {
        when(coreClient.forward(eq(HttpMethod.GET), eq("/internal/api/v1/banners"), any(), eq(Object.class)))
                .thenReturn(ResponseEntity.ok("[{\"id\":\"test-banner\"}]"));

        mockMvc.perform(get("/api/v1/banners"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/v1/banners requires STAFF or ADMIN; blocks Anonymous and CUSTOMER")
    void testPostBannerSecurity() throws Exception {
        UUID userId = UUID.randomUUID();
        String jsonPayload = "{\"title\":\"Sale\",\"imageUrl\":\"http://minio/b.png\"}";

        // 1. Anonymous -> 401
        mockMvc.perform(post("/api/v1/banners")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        // 2. CUSTOMER -> 403
        String customerToken = jwtTokenProvider.generateAccessToken(userId, "cust@techmart.vn", "CUSTOMER");
        mockMvc.perform(post("/api/v1/banners")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        // 3. STAFF -> 201 Created (proxied)
        String staffToken = jwtTokenProvider.generateAccessToken(userId, "staff@techmart.vn", "STAFF");
        when(coreClient.forward(eq(HttpMethod.POST), eq("/internal/api/v1/banners"), any(), eq(Object.class)))
                .thenReturn(ResponseEntity.status(201).body("{\"id\":\"banner-id\"}"));

        mockMvc.perform(post("/api/v1/banners")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isCreated());

        // 4. ADMIN -> 201 Created (proxied)
        String adminToken = jwtTokenProvider.generateAccessToken(userId, "admin@techmart.vn", "ADMIN");
        mockMvc.perform(post("/api/v1/banners")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("DELETE /api/v1/banners/{id} requires ADMIN role; blocks Anonymous, CUSTOMER, and STAFF")
    void testDeleteBannerSecurityAdminOnly() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID bannerId = UUID.randomUUID();

        // 1. Anonymous -> 401
        mockMvc.perform(delete("/api/v1/banners/" + bannerId))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        // 2. CUSTOMER -> 403
        String customerToken = jwtTokenProvider.generateAccessToken(userId, "cust@techmart.vn", "CUSTOMER");
        mockMvc.perform(delete("/api/v1/banners/" + bannerId)
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        // 3. STAFF -> 403 (ADMIN only)
        String staffToken = jwtTokenProvider.generateAccessToken(userId, "staff@techmart.vn", "STAFF");
        mockMvc.perform(delete("/api/v1/banners/" + bannerId)
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        // 4. ADMIN -> 204 No Content
        String adminToken = jwtTokenProvider.generateAccessToken(userId, "admin@techmart.vn", "ADMIN");
        when(coreClient.forward(eq(HttpMethod.DELETE), eq("/internal/api/v1/banners/" + bannerId), any(), eq(Object.class)))
                .thenReturn(ResponseEntity.noContent().build());

        mockMvc.perform(delete("/api/v1/banners/" + bannerId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());
    }
}
