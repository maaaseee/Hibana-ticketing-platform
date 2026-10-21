package com.aieta.ticketing_app_catalog.infrastructure;

import com.aieta.ticketing_app_catalog.infrastructure.database.entity.Auditable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Unit Test: Auditable MappedSuperclass Lifecycle Hooks")
class AuditableLifecycleTest {

    static class SampleEntity extends Auditable {
        SampleEntity() {
            super();
        }

        SampleEntity(Instant created, Instant updated) {
            super(created, updated);
        }
    }

    @Test
    @DisplayName("Should populate createdAt and updatedAt on pre-persist when null")
    void shouldPopulateTimestampsOnPrePersist() throws Exception {
        SampleEntity entity = new SampleEntity();
        assertNull(entity.getCreatedAt());
        assertNull(entity.getUpdatedAt());

        Method prePersist = Auditable.class.getDeclaredMethod("onPrePersist");
        prePersist.setAccessible(true);
        prePersist.invoke(entity);

        assertNotNull(entity.getCreatedAt(), "createdAt should be populated on prePersist");
        assertNotNull(entity.getUpdatedAt(), "updatedAt should be populated on prePersist");
        assertEquals(entity.getCreatedAt(), entity.getUpdatedAt(), "Initial updatedAt should equal createdAt");
    }

    @Test
    @DisplayName("Should preserve existing createdAt on pre-persist if already set")
    void shouldPreserveExistingCreatedAt() throws Exception {
        Instant established = Instant.parse("2026-01-01T00:00:00Z");
        SampleEntity entity = new SampleEntity(established, null);

        Method prePersist = Auditable.class.getDeclaredMethod("onPrePersist");
        prePersist.setAccessible(true);
        prePersist.invoke(entity);

        assertEquals(established, entity.getCreatedAt(), "Established createdAt should not be overwritten");
        assertEquals(established, entity.getUpdatedAt(), "updatedAt should align with established createdAt");
    }

    @Test
    @DisplayName("Should advance updatedAt on pre-update")
    void shouldAdvanceUpdatedAtOnPreUpdate() throws Exception {
        Instant initial = Instant.parse("2026-01-01T00:00:00Z");
        SampleEntity entity = new SampleEntity(initial, initial);

        Method preUpdate = Auditable.class.getDeclaredMethod("onPreUpdate");
        preUpdate.setAccessible(true);
        preUpdate.invoke(entity);

        assertEquals(initial, entity.getCreatedAt(), "createdAt must remain untouched on update");
        assertTrue(entity.getUpdatedAt().isAfter(initial), "updatedAt must advance on update");
    }
}
