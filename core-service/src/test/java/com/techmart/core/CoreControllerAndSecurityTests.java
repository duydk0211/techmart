package com.techmart.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.techmart.core.dto.request.auth.ChangePasswordRequest;
import com.techmart.core.dto.request.auth.LogoutRequest;
import com.techmart.core.dto.request.auth.RegisterRequest;
import com.techmart.core.dto.request.user.AddressRequest;
import com.techmart.core.dto.request.user.CreateStaffRequest;
import com.techmart.core.dto.response.user.UserProfileResponse;
import com.techmart.core.repository.auth.RefreshTokenRepository;
import com.techmart.core.repository.user.UserRepository;
import com.techmart.core.service.command.auth.AuthCommandService;
import com.techmart.shared.constant.HeaderConstants;
import com.techmart.shared.constant.ResponseCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CoreControllerAndSecurityTests {

    private static final String VALID_INTERNAL_SECRET = "techmart_internal_secret_2026";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private AuthCommandService authCommandService;

    @BeforeEach
    void setUp() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("InternalAuthFilter: Should reject requests without X-Internal-Secret with 403 INVALID_INTERNAL_SECRET")
    void testMissingInternalSecretRejected() throws Exception {
        mockMvc.perform(post("/internal/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ResponseCode.INVALID_INTERNAL_SECRET.getCode()))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.timestamp").value(notNullValue()))
                .andExpect(jsonPath("$.path").value("/internal/api/v1/auth/register"));
    }

    @Test
    @DisplayName("InternalAuthFilter: Should reject requests with invalid X-Internal-Secret with 403")
    void testWrongInternalSecretRejected() throws Exception {
        mockMvc.perform(post("/internal/api/v1/auth/register")
                        .header(HeaderConstants.X_INTERNAL_SECRET, "wrong_secret_value")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ResponseCode.INVALID_INTERNAL_SECRET.getCode()));
    }

    @Test
    @DisplayName("Validation: Should reject registration with invalid email format with 400 VALIDATION_FAILED")
    void testRegisterValidationInvalidEmail() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "invalid-email-format",
                "Password123@",
                "Nguyen Van Test",
                "0987654321"
        );

        mockMvc.perform(post("/internal/api/v1/auth/register")
                        .header(HeaderConstants.X_INTERNAL_SECRET, VALID_INTERNAL_SECRET)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ResponseCode.VALIDATION_FAILED.getCode()))
                .andExpect(jsonPath("$.errors[*].field", hasItem("email")));
    }

    @Test
    @DisplayName("Validation: Should reject registration with weak password with 400 VALIDATION_FAILED")
    void testRegisterValidationWeakPassword() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "weakpass@techmart.vn",
                "123",
                "Nguyen Van Test",
                "0987654321"
        );

        mockMvc.perform(post("/internal/api/v1/auth/register")
                        .header(HeaderConstants.X_INTERNAL_SECRET, VALID_INTERNAL_SECRET)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ResponseCode.VALIDATION_FAILED.getCode()))
                .andExpect(jsonPath("$.errors[*].field", hasItem("password")));
    }

    @Test
    @DisplayName("Validation: Should reject registration with invalid phone format with 400 VALIDATION_FAILED")
    void testRegisterValidationInvalidPhone() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "invalidphone@techmart.vn",
                "Password123@",
                "Nguyen Van Test",
                "12345"
        );

        mockMvc.perform(post("/internal/api/v1/auth/register")
                        .header(HeaderConstants.X_INTERNAL_SECRET, VALID_INTERNAL_SECRET)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ResponseCode.VALIDATION_FAILED.getCode()))
                .andExpect(jsonPath("$.errors[*].field", hasItem("phone")));
    }

    @Test
    @DisplayName("TechmartPreauthorize: Should reject unauthenticated call to authenticated endpoint with 401 UNAUTHORIZED")
    void testUnauthenticatedCallRejected() throws Exception {
        mockMvc.perform(get("/internal/api/v1/user/profile")
                        .header(HeaderConstants.X_INTERNAL_SECRET, VALID_INTERNAL_SECRET))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ResponseCode.UNAUTHORIZED.getCode()));
    }

    @Test
    @DisplayName("TechmartPreauthorize: Should reject CUSTOMER calling admin staff endpoint with 403 ACCESS_DENIED")
    void testCustomerForbiddenFromAdminEndpoint() throws Exception {
        UUID customerId = UUID.randomUUID();

        mockMvc.perform(get("/internal/api/v1/admin/staff")
                        .header(HeaderConstants.X_INTERNAL_SECRET, VALID_INTERNAL_SECRET)
                        .header("X-User-Id", customerId.toString())
                        .header("X-User-Role", "CUSTOMER"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ResponseCode.ACCESS_DENIED.getCode()));
    }

    @Test
    @DisplayName("TechmartPreauthorize: Should permit ADMIN to access admin staff and customer management")
    void testAdminPermittedToAdminEndpoints() throws Exception {
        UUID adminId = UUID.randomUUID();

        mockMvc.perform(get("/internal/api/v1/admin/staff")
                        .header(HeaderConstants.X_INTERNAL_SECRET, VALID_INTERNAL_SECRET)
                        .header("X-User-Id", adminId.toString())
                        .header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/internal/api/v1/admin/customers")
                        .header(HeaderConstants.X_INTERNAL_SECRET, VALID_INTERNAL_SECRET)
                        .header("X-User-Id", adminId.toString())
                        .header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Logout endpoint: Should return 204 No Content")
    void testLogoutEndpoint() throws Exception {
        RegisterRequest regReq = new RegisterRequest(
                "logouttest@techmart.vn",
                "Password123@",
                "Logout User",
                "0987654321"
        );
        UserProfileResponse user = authCommandService.register(regReq);

        LogoutRequest logoutReq = new LogoutRequest("dummy-token");

        mockMvc.perform(post("/internal/api/v1/auth/logout")
                        .header(HeaderConstants.X_INTERNAL_SECRET, VALID_INTERNAL_SECRET)
                        .header("X-User-Id", user.id().toString())
                        .header("X-User-Role", "CUSTOMER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(logoutReq)))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Change password: Should return 400 OLD_PASSWORD_INCORRECT when old password does not match")
    void testChangePasswordWrongOldPassword() throws Exception {
        RegisterRequest regReq = new RegisterRequest(
                "changepassfail@techmart.vn",
                "Password123@",
                "Change Pass User",
                "0987654321"
        );
        UserProfileResponse user = authCommandService.register(regReq);

        ChangePasswordRequest changeReq = new ChangePasswordRequest("WrongOldPass123@", "NewPassword456@");

        mockMvc.perform(post("/internal/api/v1/auth/change-password")
                        .header(HeaderConstants.X_INTERNAL_SECRET, VALID_INTERNAL_SECRET)
                        .header("X-User-Id", user.id().toString())
                        .header("X-User-Role", "CUSTOMER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(changeReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ResponseCode.OLD_PASSWORD_INCORRECT.getCode()));
    }

    @Test
    @DisplayName("Admin: Should delete (soft-delete) staff account and return 204 No Content")
    void testDeleteStaffAccount() throws Exception {
        UUID adminId = UUID.randomUUID();

        CreateStaffRequest staffReq = new CreateStaffRequest(
                "stafftodelete@techmart.vn",
                "StaffPass123@",
                "Staff To Delete",
                "0911223344"
        );

        String staffJson = mockMvc.perform(post("/internal/api/v1/admin/staff")
                        .header(HeaderConstants.X_INTERNAL_SECRET, VALID_INTERNAL_SECRET)
                        .header("X-User-Id", adminId.toString())
                        .header("X-User-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(staffReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        UserProfileResponse staffResponse = objectMapper.readValue(staffJson, UserProfileResponse.class);

        mockMvc.perform(delete("/internal/api/v1/admin/staff/" + staffResponse.id())
                        .header(HeaderConstants.X_INTERNAL_SECRET, VALID_INTERNAL_SECRET)
                        .header("X-User-Id", adminId.toString())
                        .header("X-User-Role", "ADMIN"))
                .andExpect(status().isNoContent());
    }
}
