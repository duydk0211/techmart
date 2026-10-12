package com.techmart.core.controller.auth.impl;

import com.techmart.core.authorization.UserContext;
import com.techmart.core.bus.SpringBus;
import com.techmart.core.bus.command.auth.ChangePasswordCommand;
import com.techmart.core.bus.command.auth.LoginCommand;
import com.techmart.core.bus.command.auth.LogoutCommand;
import com.techmart.core.bus.command.auth.RefreshTokenCommand;
import com.techmart.core.bus.command.auth.RegisterCommand;
import com.techmart.core.controller.auth.InternalAuthApiController;
import com.techmart.core.dto.request.auth.ChangePasswordRequest;
import com.techmart.core.dto.request.auth.LoginRequest;
import com.techmart.core.dto.request.auth.LogoutRequest;
import com.techmart.core.dto.request.auth.RefreshTokenRequest;
import com.techmart.core.dto.request.auth.RegisterRequest;
import com.techmart.core.dto.response.auth.AuthResponse;
import com.techmart.core.dto.response.user.UserProfileResponse;
import com.techmart.shared.security.TechmartPreauthorize;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/internal/api/v1/auth")
public class InternalAuthApiControllerImpl implements InternalAuthApiController {

    private static final Logger log = LoggerFactory.getLogger(InternalAuthApiControllerImpl.class);

    private final SpringBus springBus;

    public InternalAuthApiControllerImpl(SpringBus springBus) {
        this.springBus = springBus;
    }

    @Override
    @PostMapping("/register")
    @TechmartPreauthorize(gateway = "TECHMART_GW", authenticated = false)
    public ResponseEntity<UserProfileResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.info("[InternalAuthApiControllerImpl][register] Handling register request for email: {}", request.email());
        UserProfileResponse response = springBus.execute(new RegisterCommand(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Override
    @PostMapping("/login")
    @TechmartPreauthorize(gateway = "TECHMART_GW", authenticated = false)
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("[InternalAuthApiControllerImpl][login] Handling login request for email: {}", request.email());
        AuthResponse response = springBus.execute(new LoginCommand(request));
        return ResponseEntity.ok(response);
    }

    @Override
    @PostMapping("/refresh")
    @TechmartPreauthorize(gateway = "TECHMART_GW", authenticated = false)
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        log.info("[InternalAuthApiControllerImpl][refresh] Handling refresh token request");
        AuthResponse response = springBus.execute(new RefreshTokenCommand(request));
        return ResponseEntity.ok(response);
    }

    @Override
    @PostMapping("/logout")
    @TechmartPreauthorize(gateway = "TECHMART_GW", authenticated = true)
    public ResponseEntity<Void> logout(@Valid @RequestBody LogoutRequest request) {
        UUID userId = UserContext.getCurrentUser() != null ? UserContext.getCurrentUser().userId() : null;
        log.info("[InternalAuthApiControllerImpl][logout] Handling logout request for user: {}", userId);
        springBus.execute(new LogoutCommand(request, userId));
        return ResponseEntity.noContent().build();
    }

    @Override
    @PostMapping("/change-password")
    @TechmartPreauthorize(gateway = "TECHMART_GW", authenticated = true)
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        UUID userId = UserContext.getCurrentUser() != null ? UserContext.getCurrentUser().userId() : null;
        log.info("[InternalAuthApiControllerImpl][changePassword] Handling change password request for user: {}", userId);
        springBus.execute(new ChangePasswordCommand(userId, request));
        return ResponseEntity.ok().build();
    }
}
