package com.techmart.shared.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom annotation for internal authorization across TechMart services.
 * <p>
 * Enforces two-tier authorization:
 * <ol>
 *   <li>Trust-boundary verification: ensures the request originates from a trusted Gateway ({@code gateway = "TECHMART_GW"}).</li>
 *   <li>Role-based access control: validates that the delegated user role matches allowed roles ({@code roles = {"CUSTOMER", "USER"}}).</li>
 * </ol>
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface TechmartPreauthorize {

    /**
     * Required trusted Gateway identifier. Default is {@code "TECHMART_GW"}.
     */
    String gateway() default "TECHMART_GW";

    /**
     * Allowed delegated user roles (e.g., {@code {"CUSTOMER", "USER"}}, {@code {"ADMIN"}}).
     * Empty array indicates all roles passing through Gateway are accepted.
     */
    String[] roles() default {};

    /**
     * Whether the request requires an authenticated user session.
     * Default is {@code true}. Set to {@code false} for public queries routed via Gateway.
     */
    boolean authenticated() default true;
}
