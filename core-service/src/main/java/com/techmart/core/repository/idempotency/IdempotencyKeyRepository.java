package com.techmart.core.repository.idempotency;

import com.techmart.core.entity.idempotency.IdempotencyKeyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKeyEntity, UUID> {

    Optional<IdempotencyKeyEntity> findByIdempotencyKeyAndUserId(String idempotencyKey, UUID userId);

    Optional<IdempotencyKeyEntity> findByIdempotencyKey(String idempotencyKey);
}
