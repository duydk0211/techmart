package com.techmart.shared.constant;

/**
 * Constants for internal HTTP headers used across TechMart services.
 */
public final class HeaderConstants {

    public static final String X_INTERNAL_SECRET = "X-Internal-Secret";
    public static final String CORRELATION_ID = "Correlation-Id";
    public static final String X_CORRELATION_ID = "X-Correlation-Id";

    private HeaderConstants() {
        // Prevent instantiation
    }
}
