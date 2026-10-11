package com.techmart.core.controller.user;

import com.techmart.core.dto.request.user.AddressRequest;
import com.techmart.core.dto.request.user.UpdateProfileRequest;
import com.techmart.core.dto.response.user.AddressResponse;
import com.techmart.core.dto.response.user.UserProfileResponse;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

public interface InternalUserApiController {

    ResponseEntity<UserProfileResponse> getProfile();

    ResponseEntity<UserProfileResponse> updateProfile(UpdateProfileRequest request);

    ResponseEntity<List<AddressResponse>> getAddresses();

    ResponseEntity<AddressResponse> addAddress(AddressRequest request);

    ResponseEntity<AddressResponse> updateAddress(UUID id, AddressRequest request);

    ResponseEntity<Void> deleteAddress(UUID id);

    ResponseEntity<AddressResponse> setDefaultAddress(UUID id);
}
