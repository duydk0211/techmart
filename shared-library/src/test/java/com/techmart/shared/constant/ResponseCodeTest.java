package com.techmart.shared.constant;

import org.junit.jupiter.api.Test;

import java.util.Locale;
import java.util.ResourceBundle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResponseCodeTest {

    @Test
    void shouldContainExactly34EnumValues() {
        assertEquals(34, ResponseCode.values().length, "System must have exactly 34 standardized ResponseCode enums");
    }

    @Test
    void shouldHaveValidAttributesForAllEnumValues() {
        for (ResponseCode code : ResponseCode.values()) {
            assertNotNull(code.getCode(), "Code cannot be null for enum: " + code.name());
            assertEquals(code.name(), code.getCode(), "Code string must match enum constant name");
            assertNotNull(code.getHttpStatus(), "HttpStatus cannot be null for enum: " + code.name());
            assertNotNull(code.getMessageKey(), "MessageKey cannot be null for enum: " + code.name());
            assertFalse(code.getMessageKey().isBlank(), "MessageKey cannot be blank for enum: " + code.name());
        }
    }

    @Test
    void shouldEnsureAllEnumKeysExistInBothResourceBundles() {
        ResourceBundle enBundle = ResourceBundle.getBundle("messages/messages", Locale.ENGLISH);
        ResourceBundle viBundle = ResourceBundle.getBundle("messages/messages", Locale.forLanguageTag("vi"));

        for (ResponseCode code : ResponseCode.values()) {
            String key = code.getMessageKey();
            assertTrue(enBundle.containsKey(key), "Missing key in English bundle: " + key);
            assertTrue(viBundle.containsKey(key), "Missing key in Vietnamese bundle: " + key);
            assertFalse(enBundle.getString(key).isBlank(), "Empty string in English bundle for key: " + key);
            assertFalse(viBundle.getString(key).isBlank(), "Empty string in Vietnamese bundle for key: " + key);
        }
    }
}
