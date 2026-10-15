package com.techmart.core.handler.auth;

import com.techmart.core.bus.CommandHandler;
import com.techmart.core.bus.command.auth.ChangePasswordCommand;
import com.techmart.core.bus.command.auth.LoginCommand;
import com.techmart.core.bus.command.auth.LogoutCommand;
import com.techmart.core.bus.command.auth.RefreshTokenCommand;
import com.techmart.core.bus.command.auth.RegisterCommand;
import com.techmart.core.dto.response.auth.AuthResponse;
import com.techmart.core.dto.response.user.UserProfileResponse;
import com.techmart.core.service.command.auth.AuthCommandService;
import org.springframework.stereotype.Component;

public class AuthHandler {

    @Component
    public static class RegisterCommandHandler implements CommandHandler<RegisterCommand, UserProfileResponse> {
        private final AuthCommandService authCommandService;

        public RegisterCommandHandler(AuthCommandService authCommandService) {
            this.authCommandService = authCommandService;
        }

        @Override
        public UserProfileResponse handle(RegisterCommand command) {
            return authCommandService.register(command.request());
        }
    }

    @Component
    public static class LoginCommandHandler implements CommandHandler<LoginCommand, AuthResponse> {
        private final AuthCommandService authCommandService;

        public LoginCommandHandler(AuthCommandService authCommandService) {
            this.authCommandService = authCommandService;
        }

        @Override
        public AuthResponse handle(LoginCommand command) {
            return authCommandService.login(command.request());
        }
    }

    @Component
    public static class RefreshTokenCommandHandler implements CommandHandler<RefreshTokenCommand, AuthResponse> {
        private final AuthCommandService authCommandService;

        public RefreshTokenCommandHandler(AuthCommandService authCommandService) {
            this.authCommandService = authCommandService;
        }

        @Override
        public AuthResponse handle(RefreshTokenCommand command) {
            return authCommandService.refreshToken(command.request());
        }
    }

    @Component
    public static class LogoutCommandHandler implements CommandHandler<LogoutCommand, Void> {
        private final AuthCommandService authCommandService;

        public LogoutCommandHandler(AuthCommandService authCommandService) {
            this.authCommandService = authCommandService;
        }

        @Override
        public Void handle(LogoutCommand command) {
            authCommandService.logout(command.request(), command.userId());
            return null;
        }
    }

    @Component
    public static class ChangePasswordCommandHandler implements CommandHandler<ChangePasswordCommand, Void> {
        private final AuthCommandService authCommandService;

        public ChangePasswordCommandHandler(AuthCommandService authCommandService) {
            this.authCommandService = authCommandService;
        }

        @Override
        public Void handle(ChangePasswordCommand command) {
            authCommandService.changePassword(command.userId(), command.request());
            return null;
        }
    }
}
