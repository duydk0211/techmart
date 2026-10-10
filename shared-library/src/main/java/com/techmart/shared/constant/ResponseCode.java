package com.techmart.shared.constant;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Standard system-wide response and error codes for TechMart.
 */
@Getter
public enum ResponseCode {

    // --- 1. SUCCESS RESPONSES ---
    SUCCESS("SUCCESS", HttpStatus.OK, "response.success"),
    CREATED("CREATED", HttpStatus.CREATED, "response.created"),

    // --- 2. SYSTEM & VALIDATION ERRORS ---
    VALIDATION_FAILED("VALIDATION_FAILED", HttpStatus.BAD_REQUEST, "error.validation_failed"),
    INTERNAL_SERVER_ERROR("INTERNAL_SERVER_ERROR", HttpStatus.INTERNAL_SERVER_ERROR, "error.internal_server_error"),
    MALFORMED_JSON("MALFORMED_JSON", HttpStatus.BAD_REQUEST, "error.malformed_json"),
    INVALID_SORT_PARAMETER("INVALID_SORT_PARAMETER", HttpStatus.BAD_REQUEST, "error.invalid_sort_parameter"),
    RATE_LIMIT_EXCEEDED("RATE_LIMIT_EXCEEDED", HttpStatus.TOO_MANY_REQUESTS, "error.rate_limit_exceeded"),

    // --- 3. AUTHENTICATION & AUTHORIZATION ERRORS ---
    UNAUTHORIZED("UNAUTHORIZED", HttpStatus.UNAUTHORIZED, "error.unauthorized"),
    ACCESS_DENIED("ACCESS_DENIED", HttpStatus.FORBIDDEN, "error.access_denied"),
    FORBIDDEN_RESOURCE("FORBIDDEN_RESOURCE", HttpStatus.FORBIDDEN, "error.forbidden_resource"),
    INVALID_CREDENTIALS("INVALID_CREDENTIALS", HttpStatus.UNAUTHORIZED, "error.invalid_credentials"),
    ACCOUNT_TEMPORARILY_LOCKED("ACCOUNT_TEMPORARILY_LOCKED", HttpStatus.LOCKED, "error.account_temporarily_locked"),
    TOKEN_EXPIRED("TOKEN_EXPIRED", HttpStatus.UNAUTHORIZED, "error.token_expired"),
    TOKEN_REVOKED("TOKEN_REVOKED", HttpStatus.UNAUTHORIZED, "error.token_revoked"),
    INVALID_INTERNAL_SECRET("INVALID_INTERNAL_SECRET", HttpStatus.FORBIDDEN, "error.invalid_internal_secret"),

    // --- 4. USER & ADDRESS ERRORS ---
    USER_NOT_FOUND("USER_NOT_FOUND", HttpStatus.NOT_FOUND, "error.user_not_found"),
    EMAIL_ALREADY_EXISTS("EMAIL_ALREADY_EXISTS", HttpStatus.CONFLICT, "error.email_already_exists"),
    ADDRESS_LIMIT_EXCEEDED("ADDRESS_LIMIT_EXCEEDED", HttpStatus.BAD_REQUEST, "error.address_limit_exceeded"),
    ADDRESS_NOT_FOUND("ADDRESS_NOT_FOUND", HttpStatus.NOT_FOUND, "error.address_not_found"),
    OLD_PASSWORD_INCORRECT("OLD_PASSWORD_INCORRECT", HttpStatus.BAD_REQUEST, "error.old_password_incorrect"),

    // --- 5. CATALOG & INVENTORY ERRORS ---
    RESOURCE_NOT_FOUND("RESOURCE_NOT_FOUND", HttpStatus.NOT_FOUND, "error.resource_not_found"),
    DUPLICATE_RESOURCE("DUPLICATE_RESOURCE", HttpStatus.CONFLICT, "error.duplicate_resource"),
    INSUFFICIENT_STOCK("INSUFFICIENT_STOCK", HttpStatus.CONFLICT, "error.insufficient_stock"),
    CATEGORY_CYCLE_DETECTED("CATEGORY_CYCLE_DETECTED", HttpStatus.BAD_REQUEST, "error.category_cycle_detected"),
    CATEGORY_MAX_LEVEL_EXCEEDED("CATEGORY_MAX_LEVEL_EXCEEDED", HttpStatus.BAD_REQUEST, "error.category_max_level_exceeded"),
    DELETION_RESTRICTED("DELETION_RESTRICTED", HttpStatus.UNPROCESSABLE_ENTITY, "error.deletion_restricted"),
    CONCURRENT_MODIFICATION("CONCURRENT_MODIFICATION", HttpStatus.CONFLICT, "error.concurrent_modification"),

    // --- 6. ORDER ERRORS ---
    INVALID_ORDER_STATE("INVALID_ORDER_STATE", HttpStatus.UNPROCESSABLE_ENTITY, "error.invalid_order_state"),
    ORDER_CANNOT_BE_CANCELLED("ORDER_CANNOT_BE_CANCELLED", HttpStatus.UNPROCESSABLE_ENTITY, "error.order_cannot_be_cancelled");

    private final String code;
    private final HttpStatus httpStatus;
    private final String messageKey;

    ResponseCode(String code, HttpStatus httpStatus, String messageKey) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.messageKey = messageKey;
    }
}
