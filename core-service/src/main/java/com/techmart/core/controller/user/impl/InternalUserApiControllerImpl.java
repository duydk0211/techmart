package com.techmart.core.controller.user.impl;

import com.techmart.core.authorization.UserContext;
import com.techmart.core.bus.SpringBus;
import com.techmart.core.bus.command.user.AddAddressCommand;
import com.techmart.core.bus.command.user.DeleteAddressCommand;
import com.techmart.core.bus.command.user.SetDefaultAddressCommand;
import com.techmart.core.bus.command.user.UpdateAddressCommand;
import com.techmart.core.bus.command.user.UpdateProfileCommand;
import com.techmart.core.bus.query.user.GetUserAddressesQuery;
import com.techmart.core.bus.query.user.GetUserProfileQuery;
import com.techmart.core.controller.user.InternalUserApiController;
import com.techmart.core.dto.request.user.AddressRequest;
import com.techmart.core.dto.request.user.UpdateProfileRequest;
import com.techmart.core.dto.response.user.AddressResponse;
import com.techmart.core.dto.response.user.UserProfileResponse;
import com.techmart.shared.constant.ResponseCode;
import com.techmart.shared.exception.CoreException;
import com.techmart.shared.security.TechmartPreauthorize;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/internal/api/v1/user")
public class InternalUserApiControllerImpl implements InternalUserApiController {

    private static final Logger log = LoggerFactory.getLogger(InternalUserApiControllerImpl.class);

    private final SpringBus springBus;

    public InternalUserApiControllerImpl(SpringBus springBus) {
        this.springBus = springBus;
    }

    @Override
    @GetMapping("/profile")
    @TechmartPreauthorize(gateway = "TECHMART_GW", authenticated = true)
    public ResponseEntity<UserProfileResponse> getProfile() {
        UUID userId = getCurrentUserId();
        log.info("[InternalUserApiControllerImpl][getProfile] Fetching profile for user: {}", userId);
        UserProfileResponse response = springBus.query(new GetUserProfileQuery(userId));
        return ResponseEntity.ok(response);
    }

    @Override
    @PutMapping("/profile")
    @TechmartPreauthorize(gateway = "TECHMART_GW", authenticated = true)
    public ResponseEntity<UserProfileResponse> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        UUID userId = getCurrentUserId();
        log.info("[InternalUserApiControllerImpl][updateProfile] Updating profile for user: {}", userId);
        UserProfileResponse response = springBus.execute(new UpdateProfileCommand(userId, request));
        return ResponseEntity.ok(response);
    }

    @Override
    @GetMapping("/addresses")
    @TechmartPreauthorize(gateway = "TECHMART_GW", roles = {"CUSTOMER"}, authenticated = true)
    public ResponseEntity<List<AddressResponse>> getAddresses() {
        UUID userId = getCurrentUserId();
        log.info("[InternalUserApiControllerImpl][getAddresses] Fetching addresses for user: {}", userId);
        List<AddressResponse> response = springBus.query(new GetUserAddressesQuery(userId));
        return ResponseEntity.ok(response);
    }

    @Override
    @PostMapping("/addresses")
    @TechmartPreauthorize(gateway = "TECHMART_GW", roles = {"CUSTOMER"}, authenticated = true)
    public ResponseEntity<AddressResponse> addAddress(@Valid @RequestBody AddressRequest request) {
        UUID userId = getCurrentUserId();
        log.info("[InternalUserApiControllerImpl][addAddress] Adding address for user: {}", userId);
        AddressResponse response = springBus.execute(new AddAddressCommand(userId, request));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Override
    @PutMapping("/addresses/{id}")
    @TechmartPreauthorize(gateway = "TECHMART_GW", roles = {"CUSTOMER"}, authenticated = true)
    public ResponseEntity<AddressResponse> updateAddress(@PathVariable("id") UUID id, @Valid @RequestBody AddressRequest request) {
        UUID userId = getCurrentUserId();
        log.info("[InternalUserApiControllerImpl][updateAddress] Updating address {} for user: {}", id, userId);
        AddressResponse response = springBus.execute(new UpdateAddressCommand(userId, id, request));
        return ResponseEntity.ok(response);
    }

    @Override
    @DeleteMapping("/addresses/{id}")
    @TechmartPreauthorize(gateway = "TECHMART_GW", roles = {"CUSTOMER"}, authenticated = true)
    public ResponseEntity<Void> deleteAddress(@PathVariable("id") UUID id) {
        UUID userId = getCurrentUserId();
        log.info("[InternalUserApiControllerImpl][deleteAddress] Deleting address {} for user: {}", id, userId);
        springBus.execute(new DeleteAddressCommand(userId, id));
        return ResponseEntity.noContent().build();
    }

    @Override
    @PatchMapping("/addresses/{id}/default")
    @TechmartPreauthorize(gateway = "TECHMART_GW", roles = {"CUSTOMER"}, authenticated = true)
    public ResponseEntity<AddressResponse> setDefaultAddress(@PathVariable("id") UUID id) {
        UUID userId = getCurrentUserId();
        log.info("[InternalUserApiControllerImpl][setDefaultAddress] Setting default address {} for user: {}", id, userId);
        AddressResponse response = springBus.execute(new SetDefaultAddressCommand(userId, id));
        return ResponseEntity.ok(response);
    }

    private UUID getCurrentUserId() {
        UserContext.UserInfo userInfo = UserContext.getCurrentUser();
        if (userInfo == null || userInfo.userId() == null) {
            throw new CoreException(ResponseCode.UNAUTHORIZED);
        }
        return userInfo.userId();
    }
}
