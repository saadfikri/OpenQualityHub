package com.openqualityhub.entity;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class BaseEntityTest {

    // Use a concrete subclass since BaseEntity is abstract
    private static class TestEntity extends BaseEntity {
    }

    @Test
    void isDeleted_defaultFalse() {
        TestEntity entity = new TestEntity();
        assertFalse(entity.isDeleted());
    }

    @Test
    void version_defaultZero() {
        TestEntity entity = new TestEntity();
        assertEquals(0L, entity.getVersion());
    }

    @Test
    void setDeleted_shouldSetDeletedFlag() {
        TestEntity entity = new TestEntity();
        entity.setDeleted(true);
        assertTrue(entity.isDeleted());
    }

    @Test
    void id_defaultNull() {
        TestEntity entity = new TestEntity();
        assertNull(entity.getId());
    }

    @Test
    void createdAt_defaultNull() {
        TestEntity entity = new TestEntity();
        assertNull(entity.getCreatedAt());
    }

    @Test
    void updatedAt_defaultNull() {
        TestEntity entity = new TestEntity();
        assertNull(entity.getUpdatedAt());
    }

    @Test
    void createdBy_canBeSetAndRead() {
        TestEntity entity = new TestEntity();
        entity.setCreatedBy("admin@example.com");
        assertEquals("admin@example.com", entity.getCreatedBy());
    }

    @Test
    void updatedBy_canBeSetAndRead() {
        TestEntity entity = new TestEntity();
        entity.setUpdatedBy("user@example.com");
        assertEquals("user@example.com", entity.getUpdatedBy());
    }

    @Test
    void timestamps_canBeSetAndRead() {
        TestEntity entity = new TestEntity();
        Instant now = Instant.now();
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        assertEquals(now, entity.getCreatedAt());
        assertEquals(now, entity.getUpdatedAt());
    }

    @Test
    void version_canBeIncremented() {
        TestEntity entity = new TestEntity();
        entity.setVersion(1L);
        assertEquals(1L, entity.getVersion());
    }
}
