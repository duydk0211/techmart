package com.techmart.core.service.command.auth;

import com.techmart.core.dto.request.auth.ChangePasswordRequest;
import com.techmart.core.dto.request.auth.LoginRequest;
import com.techmart.core.dto.request.auth.LogoutRequest;
import com.techmart.core.dto.request.auth.RefreshTokenRequest;
import com.techmart.core.dto.request.auth.RegisterRequest;
import com.techmart.core.dto.response.auth.AuthResponse;
import com.techmart.core.dto.response.user.UserProfileResponse;

import java.util.UUID;

public interface AuthCommandService {

    UserProfileResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse refreshToken(RefreshTokenRequest request);

    void logout(LogoutRequest request, UUID userId);

    void changePassword(UUID userId, ChangePasswordRequest request);
}
