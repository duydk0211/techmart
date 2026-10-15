package com.techmart.core.controller.auth;

import com.techmart.core.dto.request.auth.ChangePasswordRequest;
import com.techmart.core.dto.request.auth.LoginRequest;
import com.techmart.core.dto.request.auth.LogoutRequest;
import com.techmart.core.dto.request.auth.RefreshTokenRequest;
import com.techmart.core.dto.request.auth.RegisterRequest;
import com.techmart.core.dto.response.auth.AuthResponse;
import com.techmart.core.dto.response.user.UserProfileResponse;
import org.springframework.http.ResponseEntity;

public interface InternalAuthApiController {

    ResponseEntity<UserProfileResponse> register(RegisterRequest request);

    ResponseEntity<AuthResponse> login(LoginRequest request);

    ResponseEntity<AuthResponse> refresh(RefreshTokenRequest request);

    ResponseEntity<Void> logout(LogoutRequest request);

    ResponseEntity<Void> changePassword(ChangePasswordRequest request);
}
