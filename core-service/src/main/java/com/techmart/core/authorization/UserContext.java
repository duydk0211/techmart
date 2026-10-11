package com.techmart.core.authorization;

import java.util.UUID;

public class UserContext {

    private static final ThreadLocal<UserInfo> CURRENT_USER = new ThreadLocal<>();

    public record UserInfo(UUID userId, String role, String email) {}

    public static void setCurrentUser(UserInfo userInfo) {
        CURRENT_USER.set(userInfo);
    }

    public static UserInfo getCurrentUser() {
        return CURRENT_USER.get();
    }

    public static void clear() {
        CURRENT_USER.remove();
    }
}
