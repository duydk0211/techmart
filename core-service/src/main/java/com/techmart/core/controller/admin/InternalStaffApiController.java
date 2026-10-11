package com.techmart.core.controller.admin;

import com.techmart.core.dto.request.user.CreateStaffRequest;
import com.techmart.core.dto.response.user.UserProfileResponse;
import com.techmart.shared.dto.PagedResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

public interface InternalStaffApiController {

    ResponseEntity<UserProfileResponse> createStaff(CreateStaffRequest request);

    ResponseEntity<PagedResponse<UserProfileResponse>> getStaffList(Pageable pageable);

    ResponseEntity<Void> deleteStaff(UUID id);
}
