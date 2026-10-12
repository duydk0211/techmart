package com.techmart.core.service.query.user;

import com.techmart.core.dto.response.user.AddressResponse;
import com.techmart.core.dto.response.user.CustomerDetailResponse;
import com.techmart.core.dto.response.user.UserProfileResponse;
import com.techmart.shared.dto.PagedResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface UserQueryService {

    UserProfileResponse getProfile(UUID userId);

    List<AddressResponse> getAddresses(UUID userId);

    PagedResponse<UserProfileResponse> getStaffList(Pageable pageable);

    PagedResponse<UserProfileResponse> getCustomers(String keyword, Pageable pageable);

    CustomerDetailResponse getCustomerDetail(UUID customerId);
}
