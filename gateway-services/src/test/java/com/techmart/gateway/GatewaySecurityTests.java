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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class GatewaySecurityTests {

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
    @DisplayName("Should permit public auth endpoints without token")
    void testPublicEndpointsPermitted() throws Exception {
        when(coreClient.forward(eq(HttpMethod.POST), eq("/internal/api/v1/auth/register"), any(), eq(Object.class)))
                .thenReturn(ResponseEntity.ok("ok"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should reject unauthenticated access to /api/v1/users/me with 401")
    void testUnauthenticatedProfileRejected() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("Should permit valid customer token to /api/v1/users/me")
    void testAuthenticatedCustomerProfileAllowed() throws Exception {
        UUID userId = UUID.randomUUID();
        String token = jwtTokenProvider.generateAccessToken(userId, "customer@techmart.vn", "CUSTOMER");

        when(coreClient.forward(eq(HttpMethod.GET), eq("/internal/api/v1/user/profile"), any(), eq(Object.class)))
                .thenReturn(ResponseEntity.ok("ok"));

        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should enforce CUSTOMER role for /api/v1/users/me/addresses/**")
    void testAddressEndpointsRoleRestricted() throws Exception {
        UUID userId = UUID.randomUUID();
        String staffToken = jwtTokenProvider.generateAccessToken(userId, "staff@techmart.vn", "STAFF");

        // STAFF role should be forbidden (403) from accessing customer address book
        mockMvc.perform(get("/api/v1/users/me/addresses")
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        String customerToken = jwtTokenProvider.generateAccessToken(userId, "customer@techmart.vn", "CUSTOMER");
        when(coreClient.forward(eq(HttpMethod.GET), eq("/internal/api/v1/user/addresses"), any(), eq(Object.class)))
                .thenReturn(ResponseEntity.ok("ok"));

        mockMvc.perform(get("/api/v1/users/me/addresses")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should deny any unregistered URL on Gateway with 403 for authenticated user")
    void testUnregisteredEndpointsDenyAll() throws Exception {
        UUID userId = UUID.randomUUID();
        String token = jwtTokenProvider.generateAccessToken(userId, "customer@techmart.vn", "CUSTOMER");

        mockMvc.perform(get("/api/v1/admin/customers")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    @DisplayName("Should enforce rate limit of 5 requests/minute on login endpoint")
    void testRateLimitOnLogin() throws Exception {
        when(coreClient.forward(eq(HttpMethod.POST), eq("/internal/api/v1/auth/login"), any(), eq(Object.class)))
                .thenReturn(ResponseEntity.ok("ok"));

        String clientIp = "192.168.1.100";

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/v1/auth/login")
                            .header("X-Forwarded-For", clientIp)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isOk());
        }

        // 6th attempt from the same IP must be rejected with 429
        mockMvc.perform(post("/api/v1/auth/login")
                        .header("X-Forwarded-For", clientIp)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "60"))
                .andExpect(jsonPath("$.code").value("RATE_LIMIT_EXCEEDED"));
    }

    @Test
    @DisplayName("Should generate Correlation-Id header on response if not provided")
    void testCorrelationIdHeaderReturned() throws Exception {
        when(coreClient.forward(eq(HttpMethod.POST), eq("/internal/api/v1/auth/register"), any(), eq(Object.class)))
                .thenReturn(ResponseEntity.ok("ok"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Correlation-Id"));
    }

    @Test
    @DisplayName("Should permit public refresh endpoint without token")
    void testRefreshEndpointPermittedWithoutToken() throws Exception {
        when(coreClient.forward(eq(HttpMethod.POST), eq("/internal/api/v1/auth/refresh"), any(), eq(Object.class)))
                .thenReturn(ResponseEntity.ok("ok"));

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());
    }
}
