package com.techmart.core.service.query.user.impl;

import com.techmart.core.dto.response.user.AddressResponse;
import com.techmart.core.dto.response.user.CustomerDetailResponse;
import com.techmart.core.dto.response.user.UserProfileResponse;
import com.techmart.core.entity.user.Role;
import com.techmart.core.entity.user.UserEntity;
import com.techmart.core.repository.user.UserRepository;
import com.techmart.core.service.query.user.UserQueryService;
import com.techmart.shared.constant.ResponseCode;
import com.techmart.shared.dto.PagedResponse;
import com.techmart.shared.exception.CoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class UserQueryServiceImpl implements UserQueryService {

    private static final Logger log = LoggerFactory.getLogger(UserQueryServiceImpl.class);

    private final UserRepository userRepository;

    public UserQueryServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserProfileResponse getProfile(UUID userId) {
        log.info("[UserQueryServiceImpl][getProfile] Fetching profile for user: {}", userId);
        UserEntity user = userRepository.findByIdAndDeletedFalse(userId)
                .orElseThrow(() -> new CoreException(ResponseCode.USER_NOT_FOUND));

        return UserProfileResponse.fromEntity(user);
    }

    @Override
    public List<AddressResponse> getAddresses(UUID userId) {
        log.info("[UserQueryServiceImpl][getAddresses] Fetching addresses for user: {}", userId);
        UserEntity user = userRepository.findByIdAndDeletedFalse(userId)
                .orElseThrow(() -> new CoreException(ResponseCode.USER_NOT_FOUND));

        return user.getUserAddressInfo() != null
                ? user.getUserAddressInfo().stream().map(AddressResponse::fromAddressItem).toList()
                : List.of();
    }

    @Override
    public PagedResponse<UserProfileResponse> getStaffList(Pageable pageable) {
        log.info("[UserQueryServiceImpl][getStaffList] Fetching staff list page: {}, size: {}", pageable.getPageNumber(), pageable.getPageSize());
        Page<UserEntity> page = userRepository.findByRoleAndDeletedFalse(Role.STAFF, pageable);

        List<UserProfileResponse> content = page.getContent().stream()
                .map(UserProfileResponse::fromEntity)
                .toList();

        return PagedResponse.of(content, page.getNumber(), page.getSize(), page.getTotalElements());
    }

    @Override
    public PagedResponse<UserProfileResponse> getCustomers(String keyword, Pageable pageable) {
        log.info("[UserQueryServiceImpl][getCustomers] Fetching customers with keyword: {}, page: {}, size: {}", keyword, pageable.getPageNumber(), pageable.getPageSize());
        Page<UserEntity> page = userRepository.searchCustomers(keyword, pageable);

        List<UserProfileResponse> content = page.getContent().stream()
                .map(UserProfileResponse::fromEntity)
                .toList();

        return PagedResponse.of(content, page.getNumber(), page.getSize(), page.getTotalElements());
    }

    @Override
    public CustomerDetailResponse getCustomerDetail(UUID customerId) {
        log.info("[UserQueryServiceImpl][getCustomerDetail] Fetching customer detail for: {}", customerId);
        UserEntity user = userRepository.findByIdAndDeletedFalse(customerId)
                .orElseThrow(() -> new CoreException(ResponseCode.USER_NOT_FOUND));

        return CustomerDetailResponse.fromEntity(user);
    }
}
