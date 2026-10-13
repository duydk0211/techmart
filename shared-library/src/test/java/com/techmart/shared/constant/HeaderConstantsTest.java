package com.techmart.shared.constant;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HeaderConstantsTest {

    @Test
    void shouldDefineCorrectHeaderConstants() {
        assertEquals("X-Internal-Secret", HeaderConstants.X_INTERNAL_SECRET);
        assertEquals("Correlation-Id", HeaderConstants.CORRELATION_ID);
        assertEquals("X-Correlation-Id", HeaderConstants.X_CORRELATION_ID);
        assertEquals("Idempotency-Key", HeaderConstants.IDEMPOTENCY_KEY);
    }

    @Test
    void shouldHavePrivateConstructorToPreventInstantiation() throws Exception {
        Constructor<HeaderConstants> constructor = HeaderConstants.class.getDeclaredConstructor();
        assertTrue(java.lang.reflect.Modifier.isPrivate(constructor.getModifiers()));
        constructor.setAccessible(true);
        HeaderConstants instance = constructor.newInstance();
        assertNotNull(instance);
    }
}
