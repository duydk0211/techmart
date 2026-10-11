package com.techmart.core.service.command.user;

import com.techmart.core.dto.request.user.AddressRequest;
import com.techmart.core.dto.request.user.CreateStaffRequest;
import com.techmart.core.dto.request.user.UpdateProfileRequest;
import com.techmart.core.dto.response.user.AddressResponse;
import com.techmart.core.dto.response.user.UserProfileResponse;

import java.util.UUID;

public interface UserCommandService {

    UserProfileResponse updateProfile(UUID userId, UpdateProfileRequest request);

    AddressResponse addAddress(UUID userId, AddressRequest request);

    AddressResponse updateAddress(UUID userId, UUID addressId, AddressRequest request);

    void deleteAddress(UUID userId, UUID addressId);

    AddressResponse setDefaultAddress(UUID userId, UUID addressId);

    UserProfileResponse createStaff(CreateStaffRequest request, UUID createdBy);

    void deleteStaff(UUID staffId);
}
