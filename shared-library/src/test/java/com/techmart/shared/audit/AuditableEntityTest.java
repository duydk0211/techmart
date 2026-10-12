package com.techmart.shared.audit;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuditableEntityTest {

    static class DummyEntity extends AuditableEntity {}

    @Test
    void shouldGenerateValidUuidV7OnPrePersist() {
        DummyEntity entity = new DummyEntity();
        assertNull(entity.getId());

        entity.onCreate();
        assertNotNull(entity.getId());
        assertEquals(7, entity.getId().version());
    }

    @Test
    void shouldPreserveExistingIdWhenAlreadySet() {
        DummyEntity entity = new DummyEntity();
        UUID fixedId = UUID.fromString("01900000-0000-7000-8000-000000000001");
        entity.setId(fixedId);

        entity.onCreate();
        assertEquals(fixedId, entity.getId());
    }

    @Test
    void shouldMaintainMonotonicOrderOverTime() throws InterruptedException {
        DummyEntity entity1 = new DummyEntity();
        entity1.onCreate();

        Thread.sleep(2);

        DummyEntity entity2 = new DummyEntity();
        entity2.onCreate();

        assertTrue(entity1.getId().compareTo(entity2.getId()) < 0);
    }

    @Test
    void shouldInitializeAndRefreshTimestamps() throws InterruptedException {
        DummyEntity entity = new DummyEntity();
        assertNull(entity.getCreatedAt());
        assertNull(entity.getUpdatedAt());

        entity.onCreate();
        Instant initialCreated = entity.getCreatedAt();
        Instant initialUpdated = entity.getUpdatedAt();

        assertNotNull(initialCreated);
        assertNotNull(initialUpdated);

        Thread.sleep(5);
        entity.onUpdate();

        assertEquals(initialCreated, entity.getCreatedAt());
        assertTrue(entity.getUpdatedAt().isAfter(initialUpdated));
    }

    @Test
    void shouldAllowSettingCreatedByAndUpdatedBy() {
        DummyEntity entity = new DummyEntity();
        UUID userId = UUID.randomUUID();

        entity.setCreatedBy(userId);
        entity.setUpdatedBy(userId);

        assertEquals(userId, entity.getCreatedBy());
        assertEquals(userId, entity.getUpdatedBy());
    }

    @Test
    void shouldAdhereToEqualsAndHashCodeContract() {
        DummyEntity entity1 = new DummyEntity();
        DummyEntity entity2 = new DummyEntity();
        UUID sharedId = UUID.randomUUID();

        entity1.setId(sharedId);
        entity2.setId(sharedId);

        // Reflexive
        assertEquals(entity1, entity1);
        // Symmetric
        assertEquals(entity1, entity2);
        assertEquals(entity2, entity1);
        assertEquals(entity1.hashCode(), entity2.hashCode());

        // Null comparisons
        assertFalse(entity1.equals(null));
        assertFalse(entity1.equals("some-other-object"));

        // Different IDs
        DummyEntity entity3 = new DummyEntity();
        entity3.setId(UUID.randomUUID());
        assertFalse(entity1.equals(entity3));

        // Both unpersisted (null ID) should NOT be equal according to JPA spec
        DummyEntity unpersisted1 = new DummyEntity();
        DummyEntity unpersisted2 = new DummyEntity();
        assertFalse(unpersisted1.equals(unpersisted2));
        assertFalse(entity1.equals(unpersisted1));
    }
}
