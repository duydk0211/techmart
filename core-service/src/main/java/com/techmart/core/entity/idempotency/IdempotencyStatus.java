package com.techmart.core.entity.idempotency;

/**
 * Lifecycle status of an idempotency key.
 */
public enum IdempotencyStatus {
    IN_PROGRESS,
    COMPLETED,
    FAILED
}
