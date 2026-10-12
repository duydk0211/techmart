package com.techmart.core;

import com.techmart.core.dto.request.auth.ChangePasswordRequest;
import com.techmart.core.dto.request.auth.LoginRequest;
import com.techmart.core.dto.request.auth.RefreshTokenRequest;
import com.techmart.core.dto.request.auth.RegisterRequest;
import com.techmart.core.dto.request.user.AddressRequest;
import com.techmart.core.dto.request.user.CreateStaffRequest;
import com.techmart.core.dto.response.auth.AuthResponse;
import com.techmart.core.dto.response.user.AddressResponse;
import com.techmart.core.dto.response.user.UserProfileResponse;
import com.techmart.core.entity.auth.RefreshTokenEntity;
import com.techmart.core.entity.user.Role;
import com.techmart.core.entity.user.UserEntity;
import com.techmart.core.repository.auth.RefreshTokenRepository;
import com.techmart.core.repository.user.UserRepository;
import com.techmart.core.service.command.auth.AuthCommandService;
import com.techmart.core.service.command.user.UserCommandService;
import com.techmart.core.service.query.user.UserQueryService;
import com.techmart.shared.constant.ResponseCode;
import com.techmart.shared.exception.CoreException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class AuthAndUserCoreServiceTests {

    @Autowired
    private AuthCommandService authCommandService;

    @Autowired
    private UserCommandService userCommandService;

    @Autowired
    private UserQueryService userQueryService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @BeforeEach
    void setUp() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Should successfully register a new customer with CUSTOMER role")
    void testRegisterSuccess() {
        RegisterRequest request = new RegisterRequest(
                "customer1@techmart.vn",
                "Password123@",
                "Nguyen Van A",
                "0987654321"
        );

        UserProfileResponse response = authCommandService.register(request);

        assertNotNull(response.id());
        assertEquals("customer1@techmart.vn", response.email());
        assertEquals("Nguyen Van A", response.fullName());
        assertEquals("CUSTOMER", response.role());

        UserEntity savedUser = userRepository.findById(response.id()).orElseThrow();
        assertFalse(savedUser.getPasswordHash().contains("Password123@"), "Password must be hashed");
    }

    @Test
    @DisplayName("Should reject registration with duplicate email")
    void testRegisterDuplicateEmail() {
        RegisterRequest request = new RegisterRequest(
                "duplicate@techmart.vn",
                "Password123@",
                "User One",
                "0987654321"
        );
        authCommandService.register(request);

        CoreException ex = assertThrows(CoreException.class, () -> authCommandService.register(request));
        assertEquals(ResponseCode.EMAIL_ALREADY_EXISTS, ex.getResponseCode());
    }

    @Test
    @DisplayName("Should successfully login with correct credentials and issue tokens")
    void testLoginSuccess() {
        RegisterRequest regReq = new RegisterRequest(
                "login@techmart.vn",
                "Password123@",
                "Login Test",
                "0987654321"
        );
        authCommandService.register(regReq);

        LoginRequest loginReq = new LoginRequest("login@techmart.vn", "Password123@");
        AuthResponse authResp = authCommandService.login(loginReq);

        assertNotNull(authResp.accessToken());
        assertNotNull(authResp.refreshToken());
        assertEquals("Bearer", authResp.tokenType());
        assertEquals("login@techmart.vn", authResp.user().email());
    }

    @Test
    @DisplayName("Should lock account for 15 minutes after 5 consecutive failed login attempts")
    void testAccountLockoutAfter5FailedAttempts() {
        RegisterRequest regReq = new RegisterRequest(
                "lockout@techmart.vn",
                "Password123@",
                "Lockout Test",
                "0987654321"
        );
        authCommandService.register(regReq);

        LoginRequest wrongLoginReq = new LoginRequest("lockout@techmart.vn", "WrongPassword!");

        // 4 failed attempts -> INVALID_CREDENTIALS
        for (int i = 0; i < 4; i++) {
            CoreException ex = assertThrows(CoreException.class, () -> authCommandService.login(wrongLoginReq));
            assertEquals(ResponseCode.INVALID_CREDENTIALS, ex.getResponseCode());
        }

        // 5th failed attempt -> ACCOUNT_TEMPORARILY_LOCKED
        CoreException fifthEx = assertThrows(CoreException.class, () -> authCommandService.login(wrongLoginReq));
        assertEquals(ResponseCode.ACCOUNT_TEMPORARILY_LOCKED, fifthEx.getResponseCode());

        // Subsequent attempt with right password should still be locked
        LoginRequest rightLoginReq = new LoginRequest("lockout@techmart.vn", "Password123@");
        CoreException lockedEx = assertThrows(CoreException.class, () -> authCommandService.login(rightLoginReq));
        assertEquals(ResponseCode.ACCOUNT_TEMPORARILY_LOCKED, lockedEx.getResponseCode());
    }

    @Test
    @DisplayName("Should rotate refresh token upon refresh")
    void testRefreshTokenRotation() {
        RegisterRequest regReq = new RegisterRequest(
                "rtr@techmart.vn",
                "Password123@",
                "RTR Test",
                "0987654321"
        );
        authCommandService.register(regReq);

        AuthResponse initialAuth = authCommandService.login(new LoginRequest("rtr@techmart.vn", "Password123@"));

        RefreshTokenRequest refreshReq = new RefreshTokenRequest(initialAuth.refreshToken());
        AuthResponse rotatedAuth = authCommandService.refreshToken(refreshReq);

        assertNotNull(rotatedAuth.accessToken());
        assertNotNull(rotatedAuth.refreshToken());
        assertFalse(initialAuth.refreshToken().equals(rotatedAuth.refreshToken()), "New refresh token must be distinct");
    }

    @Test
    @DisplayName("Should detect replay attack and revoke all tokens for user")
    void testReplayAttackDetection() {
        RegisterRequest regReq = new RegisterRequest(
                "replay@techmart.vn",
                "Password123@",
                "Replay Test",
                "0987654321"
        );
        UserProfileResponse user = authCommandService.register(regReq);

        AuthResponse initialAuth = authCommandService.login(new LoginRequest("replay@techmart.vn", "Password123@"));

        // Valid refresh consumes initial token
        authCommandService.refreshToken(new RefreshTokenRequest(initialAuth.refreshToken()));

        // Attacker replays initial token
        CoreException ex = assertThrows(CoreException.class, () ->
                authCommandService.refreshToken(new RefreshTokenRequest(initialAuth.refreshToken()))
        );
        assertEquals(ResponseCode.TOKEN_REVOKED, ex.getResponseCode());

        // Verify all refresh tokens for this user are revoked
        List<RefreshTokenEntity> tokens = refreshTokenRepository.findAll();
        assertFalse(tokens.isEmpty());
        assertTrue(tokens.stream().allMatch(RefreshTokenEntity::isRevoked));
    }

    @Test
    @DisplayName("Should manage address book with auto-default and 10 address limit")
    void testAddressBookOperations() {
        RegisterRequest regReq = new RegisterRequest(
                "address@techmart.vn",
                "Password123@",
                "Address Test",
                "0987654321"
        );
        UserProfileResponse user = authCommandService.register(regReq);
        UUID userId = user.id();

        // 1. First address is auto default even if isDefault = false
        AddressRequest addr1 = new AddressRequest("Home", "0987654321", "123 Le Loi", "District 1", "HCMC", false);
        AddressResponse resp1 = userCommandService.addAddress(userId, addr1);
        assertTrue(resp1.isDefault(), "First address must automatically be default");

        // 2. Second address added as default -> first address becomes non-default
        AddressRequest addr2 = new AddressRequest("Office", "0987654321", "456 Nguyen Hue", "District 1", "HCMC", true);
        AddressResponse resp2 = userCommandService.addAddress(userId, addr2);
        assertTrue(resp2.isDefault());

        List<AddressResponse> addressesAfter2 = userQueryService.getAddresses(userId);
        assertEquals(2, addressesAfter2.size());
        AddressResponse updatedAddr1 = addressesAfter2.stream().filter(a -> a.id().equals(resp1.id())).findFirst().orElseThrow();
        assertFalse(updatedAddr1.isDefault(), "First address must be flipped to non-default");

        // 3. Add 8 more addresses to reach 10
        for (int i = 3; i <= 10; i++) {
            AddressRequest addr = new AddressRequest("Addr " + i, "0987654321", "Street " + i, "Dist " + i, "HCMC", false);
            userCommandService.addAddress(userId, addr);
        }

        assertEquals(10, userQueryService.getAddresses(userId).size());

        // 4. 11th address must fail with ADDRESS_LIMIT_EXCEEDED
        AddressRequest addr11 = new AddressRequest("Addr 11", "0987654321", "Street 11", "Dist 11", "HCMC", false);
        CoreException ex = assertThrows(CoreException.class, () -> userCommandService.addAddress(userId, addr11));
        assertEquals(ResponseCode.ADDRESS_LIMIT_EXCEEDED, ex.getResponseCode());

        // 5. Deleting default address promotes first remaining address to default
        userCommandService.deleteAddress(userId, resp2.id());
        List<AddressResponse> remainingAddresses = userQueryService.getAddresses(userId);
        assertEquals(9, remainingAddresses.size());
        assertTrue(remainingAddresses.get(0).isDefault(), "First remaining address must become default after deleting default");
    }

    @Test
    @DisplayName("Should revoke all sessions when changing password")
    void testChangePasswordRevokesAllSessions() {
        RegisterRequest regReq = new RegisterRequest(
                "pwdchange@techmart.vn",
                "Password123@",
                "Password Change Test",
                "0987654321"
        );
        UserProfileResponse user = authCommandService.register(regReq);
        UUID userId = user.id();

        authCommandService.login(new LoginRequest("pwdchange@techmart.vn", "Password123@"));

        ChangePasswordRequest changeReq = new ChangePasswordRequest("Password123@", "NewPassword456@");
        authCommandService.changePassword(userId, changeReq);

        // Verify all refresh tokens are revoked
        List<RefreshTokenEntity> tokens = refreshTokenRepository.findAll();
        assertTrue(tokens.stream().allMatch(RefreshTokenEntity::isRevoked));

        // Can login with new password
        AuthResponse newAuth = authCommandService.login(new LoginRequest("pwdchange@techmart.vn", "NewPassword456@"));
        assertNotNull(newAuth.accessToken());
    }

    @Test
    @DisplayName("Should create staff account with STAFF role by admin")
    void testCreateStaff() {
        UUID adminId = UUID.randomUUID();
        CreateStaffRequest request = new CreateStaffRequest(
                "staff1@techmart.vn",
                "StaffPass123@",
                "Staff Member",
                "0912345678"
        );

        UserProfileResponse staff = userCommandService.createStaff(request, adminId);
        assertNotNull(staff.id());
        assertEquals(Role.STAFF.name(), staff.role());
    }
}
