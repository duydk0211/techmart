package com.techmart.shared.exception;

import com.techmart.shared.constant.ResponseCode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class CoreExceptionTest {

    @Test
    void shouldCreateCoreExceptionWithResponseCodeAndArgs() {
        Object[] args = new Object[]{"field1", 100};
        CoreException ex = new CoreException(ResponseCode.VALIDATION_FAILED, args);

        assertEquals(ResponseCode.VALIDATION_FAILED, ex.getResponseCode());
        assertEquals(ResponseCode.VALIDATION_FAILED.getCode(), ex.getMessage());
        assertArrayEquals(args, ex.getArgs());
        assertNull(ex.getCause());
    }

    @Test
    void shouldCreateCoreExceptionWithCustomMessage() {
        String customMsg = "Specific error description";
        Object[] args = new Object[]{"extra"};
        CoreException ex = new CoreException(ResponseCode.RESOURCE_NOT_FOUND, customMsg, args);

        assertEquals(ResponseCode.RESOURCE_NOT_FOUND, ex.getResponseCode());
        assertEquals(customMsg, ex.getMessage());
        assertArrayEquals(args, ex.getArgs());
    }

    @Test
    void shouldCreateCoreExceptionWithCause() {
        IllegalArgumentException cause = new IllegalArgumentException("Invalid argument");
        CoreException ex = new CoreException(ResponseCode.INTERNAL_SERVER_ERROR, cause, "arg1");

        assertEquals(ResponseCode.INTERNAL_SERVER_ERROR, ex.getResponseCode());
        assertEquals(ResponseCode.INTERNAL_SERVER_ERROR.getCode(), ex.getMessage());
        assertSame(cause, ex.getCause());
        assertArrayEquals(new Object[]{"arg1"}, ex.getArgs());
    }

    @Test
    void shouldHandleNullResponseCodeGracefully() {
        CoreException ex = new CoreException(null);

        assertNull(ex.getResponseCode());
        assertEquals("UNKNOWN_ERROR", ex.getMessage());

        CoreException exWithCause = new CoreException(null, new RuntimeException("test"));
        assertNull(exWithCause.getResponseCode());
        assertEquals("UNKNOWN_ERROR", exWithCause.getMessage());
    }
}
