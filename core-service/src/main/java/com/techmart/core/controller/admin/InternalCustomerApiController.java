package com.techmart.core.controller.admin;

import com.techmart.core.dto.response.user.CustomerDetailResponse;
import com.techmart.core.dto.response.user.UserProfileResponse;
import com.techmart.shared.dto.PagedResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

public interface InternalCustomerApiController {

    ResponseEntity<PagedResponse<UserProfileResponse>> getCustomerList(String keyword, Pageable pageable);

    ResponseEntity<CustomerDetailResponse> getCustomerDetail(UUID id);
}
