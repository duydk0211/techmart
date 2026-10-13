package com.techmart.core.service.command.idempotency;

import com.techmart.core.entity.idempotency.IdempotencyKeyEntity;

import java.util.Optional;
import java.util.UUID;

public interface IdempotencyService {

    IdempotencyKeyEntity createOrUpdateInProgress(String key, UUID userId, String requestPath, String requestHash, int expireHours);

    void markCompleted(UUID keyId, int responseStatus, String responseBodyJson);

    void markFailed(UUID keyId);

    Optional<IdempotencyKeyEntity> findByKeyAndUserId(String key, UUID userId);

    void incrementBlockedRequests();

    long getBlockedRequestsCount();
}
