package com.techmart.shared.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom annotation to enforce idempotent execution on sensitive operations.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Idempotent {

    /**
     * Expiration duration in hours for the stored idempotency key.
     * Default is 24 hours.
     */
    int expireHours() default 24;

    /**
     * Whether the Idempotency-Key header is strictly required.
     * If true and the header is missing, a validation error is thrown.
     * Default is false for backward compatibility.
     */
    boolean required() default false;
}
