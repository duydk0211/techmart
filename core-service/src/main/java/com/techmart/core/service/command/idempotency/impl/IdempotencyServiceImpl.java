package com.techmart.core.service.command.idempotency.impl;

import com.techmart.core.entity.idempotency.IdempotencyKeyEntity;
import com.techmart.core.entity.idempotency.IdempotencyStatus;
import com.techmart.core.repository.idempotency.IdempotencyKeyRepository;
import com.techmart.core.service.command.idempotency.IdempotencyService;
import com.techmart.shared.constant.ResponseCode;
import com.techmart.shared.exception.CoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class IdempotencyServiceImpl implements IdempotencyService {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyServiceImpl.class);

    private final IdempotencyKeyRepository repository;
    private final AtomicLong blockedRequestsCounter = new AtomicLong(0);

    public IdempotencyServiceImpl(IdempotencyKeyRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public IdempotencyKeyEntity createOrUpdateInProgress(String key, UUID userId, String requestPath, String requestHash, int expireHours) {
        log.info("[IdempotencyServiceImpl][createOrUpdateInProgress] Registering key: {} for user: {}", key, userId);

        Optional<IdempotencyKeyEntity> existingOpt = repository.findByIdempotencyKeyAndUserId(key, userId);

        if (existingOpt.isPresent()) {
            IdempotencyKeyEntity existing = existingOpt.get();
            boolean isExpired = existing.getExpiresAt().isBefore(Instant.now());

            if (!isExpired) {
                if (existing.getStatus() == IdempotencyStatus.IN_PROGRESS) {
                    log.warn("[IdempotencyServiceImpl][createOrUpdateInProgress] Key is already IN_PROGRESS: {}", key);
                    throw new CoreException(ResponseCode.IDEMPOTENCY_KEY_IN_PROGRESS);
                } else if (existing.getStatus() == IdempotencyStatus.COMPLETED) {
                    return existing;
                }
            }

            // If expired or FAILED, allow re-use by resetting to IN_PROGRESS
            existing.setStatus(IdempotencyStatus.IN_PROGRESS);
            existing.setRequestPath(requestPath);
            existing.setRequestHash(requestHash);
            existing.setResponseStatus(null);
            existing.setResponseBody(null);
            existing.setExpiresAt(Instant.now().plus(expireHours, ChronoUnit.HOURS));
            return repository.saveAndFlush(existing);
        }

        try {
            IdempotencyKeyEntity entity = new IdempotencyKeyEntity();
            entity.setIdempotencyKey(key);
            entity.setUserId(userId);
            entity.setRequestPath(requestPath);
            entity.setRequestHash(requestHash);
            entity.setStatus(IdempotencyStatus.IN_PROGRESS);
            entity.setExpiresAt(Instant.now().plus(expireHours, ChronoUnit.HOURS));
            return repository.saveAndFlush(entity);
        } catch (DataIntegrityViolationException ex) {
            log.warn("[IdempotencyServiceImpl][createOrUpdateInProgress] Concurrent creation conflict for key: {}", key);
            return repository.findByIdempotencyKeyAndUserId(key, userId)
                    .map(existing -> {
                        if (existing.getStatus() == IdempotencyStatus.IN_PROGRESS) {
                            throw new CoreException(ResponseCode.IDEMPOTENCY_KEY_IN_PROGRESS);
                        }
                        return existing;
                    })
                    .orElseThrow(() -> new CoreException(ResponseCode.IDEMPOTENCY_KEY_IN_PROGRESS));
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markCompleted(UUID keyId, int responseStatus, String responseBodyJson) {
        log.info("[IdempotencyServiceImpl][markCompleted] Marking keyId: {} as COMPLETED with status: {}", keyId, responseStatus);
        repository.findById(keyId).ifPresent(entity -> {
            entity.setStatus(IdempotencyStatus.COMPLETED);
            entity.setResponseStatus(responseStatus);
            entity.setResponseBody(responseBodyJson);
            repository.saveAndFlush(entity);
        });
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(UUID keyId) {
        log.warn("[IdempotencyServiceImpl][markFailed] Marking keyId: {} as FAILED", keyId);
        repository.findById(keyId).ifPresent(entity -> {
            entity.setStatus(IdempotencyStatus.FAILED);
            repository.saveAndFlush(entity);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<IdempotencyKeyEntity> findByKeyAndUserId(String key, UUID userId) {
        return repository.findByIdempotencyKeyAndUserId(key, userId);
    }

    @Override
    public void incrementBlockedRequests() {
        long count = blockedRequestsCounter.incrementAndGet();
        log.debug("[IdempotencyServiceImpl][incrementBlockedRequests] Total blocked/replayed requests: {}", count);
    }

    @Override
    public long getBlockedRequestsCount() {
        return blockedRequestsCounter.get();
    }
}
