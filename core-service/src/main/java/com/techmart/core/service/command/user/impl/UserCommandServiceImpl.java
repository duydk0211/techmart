package com.techmart.core.service.command.user.impl;

import com.techmart.core.dto.request.user.AddressRequest;
import com.techmart.core.dto.request.user.CreateStaffRequest;
import com.techmart.core.dto.request.user.UpdateProfileRequest;
import com.techmart.core.dto.response.user.AddressResponse;
import com.techmart.core.dto.response.user.UserProfileResponse;
import com.techmart.core.entity.user.AddressItem;
import com.techmart.core.entity.user.Role;
import com.techmart.core.entity.user.UserEntity;
import com.techmart.core.repository.user.UserRepository;
import com.techmart.core.service.command.user.UserCommandService;
import com.techmart.shared.constant.ResponseCode;
import com.techmart.shared.exception.CoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@Transactional
public class UserCommandServiceImpl implements UserCommandService {

    private static final Logger log = LoggerFactory.getLogger(UserCommandServiceImpl.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserCommandServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserProfileResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        log.info("[UserCommandServiceImpl][updateProfile] Updating profile for user: {}", userId);
        UserEntity user = userRepository.findByIdAndDeletedFalse(userId)
                .orElseThrow(() -> new CoreException(ResponseCode.USER_NOT_FOUND));

        user.setFullName(request.fullName());
        user.setPhone(request.phone());
        if (request.avatarUrl() != null) {
            user.setAvatarUrl(request.avatarUrl());
        }

        UserEntity savedUser = userRepository.save(user);
        return UserProfileResponse.fromEntity(savedUser);
    }

    @Override
    public AddressResponse addAddress(UUID userId, AddressRequest request) {
        log.info("[UserCommandServiceImpl][addAddress] Adding address for user: {}", userId);
        UserEntity user = userRepository.findByIdAndDeletedFalse(userId)
                .orElseThrow(() -> new CoreException(ResponseCode.USER_NOT_FOUND));

        UUID newAddressId = UUID.randomUUID();
        AddressItem newItem = new AddressItem(
                newAddressId,
                request.name(),
                request.phone(),
                request.street(),
                request.province(),
                request.city(),
                request.isDefault()
        );

        user.addAddress(newItem);
        UserEntity savedUser = userRepository.save(user);

        return savedUser.getUserAddressInfo().stream()
                .filter(item -> item.id().equals(newAddressId))
                .findFirst()
                .map(AddressResponse::fromAddressItem)
                .orElseThrow(() -> new CoreException(ResponseCode.ADDRESS_NOT_FOUND));
    }

    @Override
    public AddressResponse updateAddress(UUID userId, UUID addressId, AddressRequest request) {
        log.info("[UserCommandServiceImpl][updateAddress] Updating address {} for user: {}", addressId, userId);
        UserEntity user = userRepository.findByIdAndDeletedFalse(userId)
                .orElseThrow(() -> new CoreException(ResponseCode.USER_NOT_FOUND));

        AddressItem updatedItem = new AddressItem(
                addressId,
                request.name(),
                request.phone(),
                request.street(),
                request.province(),
                request.city(),
                request.isDefault()
        );

        user.updateAddress(addressId, updatedItem);
        UserEntity savedUser = userRepository.save(user);

        return savedUser.getUserAddressInfo().stream()
                .filter(item -> item.id().equals(addressId))
                .findFirst()
                .map(AddressResponse::fromAddressItem)
                .orElseThrow(() -> new CoreException(ResponseCode.ADDRESS_NOT_FOUND));
    }

    @Override
    public void deleteAddress(UUID userId, UUID addressId) {
        log.info("[UserCommandServiceImpl][deleteAddress] Deleting address {} for user: {}", addressId, userId);
        UserEntity user = userRepository.findByIdAndDeletedFalse(userId)
                .orElseThrow(() -> new CoreException(ResponseCode.USER_NOT_FOUND));

        user.deleteAddress(addressId);
        userRepository.save(user);
    }

    @Override
    public AddressResponse setDefaultAddress(UUID userId, UUID addressId) {
        log.info("[UserCommandServiceImpl][setDefaultAddress] Setting default address {} for user: {}", addressId, userId);
        UserEntity user = userRepository.findByIdAndDeletedFalse(userId)
                .orElseThrow(() -> new CoreException(ResponseCode.USER_NOT_FOUND));

        user.setDefaultAddress(addressId);
        UserEntity savedUser = userRepository.save(user);

        return savedUser.getUserAddressInfo().stream()
                .filter(item -> item.id().equals(addressId))
                .findFirst()
                .map(AddressResponse::fromAddressItem)
                .orElseThrow(() -> new CoreException(ResponseCode.ADDRESS_NOT_FOUND));
    }

    @Override
    public UserProfileResponse createStaff(CreateStaffRequest request, UUID createdBy) {
        log.info("[UserCommandServiceImpl][createStaff] Creating new staff with email: {}", request.email());

        if (userRepository.existsByEmailAndDeletedFalse(request.email())) {
            log.warn("[UserCommandServiceImpl][createStaff] Email already exists: {}", request.email());
            throw new CoreException(ResponseCode.EMAIL_ALREADY_EXISTS);
        }

        UserEntity user = new UserEntity();
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFullName(request.fullName());
        user.setPhone(request.phone());
        user.setRole(Role.STAFF);
        user.setCreatedBy(createdBy);

        UserEntity savedUser = userRepository.save(user);
        return UserProfileResponse.fromEntity(savedUser);
    }

    @Override
    public void deleteStaff(UUID staffId) {
        log.info("[UserCommandServiceImpl][deleteStaff] Deactivating staff user: {}", staffId);
        UserEntity user = userRepository.findByIdAndDeletedFalse(staffId)
                .orElseThrow(() -> new CoreException(ResponseCode.USER_NOT_FOUND));

        user.setDeleted(true);
        user.setDeletedAt(Instant.now());
        userRepository.save(user);
    }
}
