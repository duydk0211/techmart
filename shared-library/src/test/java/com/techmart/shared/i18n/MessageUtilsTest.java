package com.techmart.shared.i18n;

import com.techmart.shared.constant.ResponseCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.ResourceBundleMessageSource;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MessageUtilsTest {

    @BeforeEach
    void setUp() {
        ResourceBundleMessageSource source = new ResourceBundleMessageSource();
        source.setBasename("messages/messages");
        source.setDefaultEncoding("UTF-8");
        source.setUseCodeAsDefaultMessage(true);
        new MessageUtils(source);
    }

    @AfterEach
    void tearDown() {
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    void shouldReturnEnglishMessageByDefault() {
        LocaleContextHolder.setLocale(Locale.ENGLISH);
        String msg = MessageUtils.getMessage(ResponseCode.SUCCESS);
        assertEquals("Operation completed successfully.", msg);
    }

    @Test
    void shouldReturnVietnameseMessageWhenLocaleIsVi() {
        LocaleContextHolder.setLocale(Locale.forLanguageTag("vi"));
        String msg = MessageUtils.getMessage(ResponseCode.SUCCESS);
        assertEquals("Thao tác thành công.", msg);
    }

    @Test
    void shouldFallbackToKeyWhenKeyNotFound() {
        String msg = MessageUtils.getMessage("non.existent.key");
        assertEquals("non.existent.key", msg);
    }

    @Test
    void shouldSupportDirectLocaleArgument() {
        String enMsg = MessageUtils.getMessage(Locale.ENGLISH, "response.created");
        String viMsg = MessageUtils.getMessage(Locale.forLanguageTag("vi"), "response.created");

        assertEquals("Resource created successfully.", enMsg);
        assertEquals("Tạo mới tài nguyên thành công.", viMsg);
    }

    @Test
    void shouldReturnEmptyStringWhenKeyOrResponseCodeIsNull() {
        assertEquals("", MessageUtils.getMessage((String) null));
        assertEquals("", MessageUtils.getMessage((ResponseCode) null));
        assertEquals("", MessageUtils.getMessage(Locale.ENGLISH, null));
    }

    @Test
    void shouldFallbackSafelyWhenMessageSourceIsNull() {
        // Temporarily nullify the static messageSource
        new MessageUtils(null);

        assertEquals("test.fallback.key", MessageUtils.getMessage("test.fallback.key"));
        assertEquals("test.fallback.key", MessageUtils.getMessage(Locale.ENGLISH, "test.fallback.key"));
        assertEquals("", MessageUtils.getMessage((String) null));
        assertEquals("", MessageUtils.getMessage((ResponseCode) null));
        assertEquals("response.success", MessageUtils.getMessage(ResponseCode.SUCCESS));
    }
}
