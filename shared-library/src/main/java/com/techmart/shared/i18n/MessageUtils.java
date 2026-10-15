package com.techmart.shared.i18n;

import com.techmart.shared.constant.ResponseCode;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Utility class for resolving localized messages with safe fallback support.
 */
@Component
public class MessageUtils {

    private static MessageSource messageSource;

    public MessageUtils(MessageSource messageSource) {
        MessageUtils.messageSource = messageSource;
    }

    public static String getMessage(String key, Object... args) {
        if (messageSource == null || key == null) {
            return key != null ? key : "";
        }
        try {
            Locale currentLocale = LocaleContextHolder.getLocale();
            return messageSource.getMessage(key, args, currentLocale);
        } catch (Exception e) {
            return key;
        }
    }

    public static String getMessage(Locale locale, String key, Object... args) {
        if (messageSource == null || key == null) {
            return key != null ? key : "";
        }
        try {
            return messageSource.getMessage(key, args, locale != null ? locale : Locale.ENGLISH);
        } catch (Exception e) {
            return key;
        }
    }

    public static String getMessage(ResponseCode responseCode, Object... args) {
        if (responseCode == null) {
            return "";
        }
        return getMessage(responseCode.getMessageKey(), args);
    }
}
