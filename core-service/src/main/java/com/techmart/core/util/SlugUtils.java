package com.techmart.core.util;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Utility for normalizing Vietnamese and Latin strings into SEO-friendly URL slugs.
 */
public final class SlugUtils {

    private static final Pattern NON_LATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]+");
    private static final Pattern MULTI_HYPHEN = Pattern.compile("-+");

    private SlugUtils() {
        // Utility class
    }

    /**
     * Converts a given text into a normalized SEO slug.
     * E.g. "iPhone 15 Pro Max 256GB" -> "iphone-15-pro-max-256gb"
     * "Điện thoại & Phụ kiện" -> "dien-thoai-phu-kien"
     *
     * @param input Raw text to convert
     * @return Lowercase hyphenated slug
     */
    public static String toSlug(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }

        // Replace specific Vietnamese characters before decomposition
        String normalized = input.trim()
                .replace("đ", "d")
                .replace("Đ", "d");

        // Decompose diacritics
        normalized = Normalizer.normalize(normalized, Normalizer.Form.NFD);
        normalized = normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");

        // Convert spaces to hyphens and remove non-alphanumeric chars
        normalized = WHITESPACE.matcher(normalized).replaceAll("-");
        normalized = normalized.toLowerCase(Locale.ROOT);
        normalized = NON_LATIN.matcher(normalized).replaceAll("");
        normalized = MULTI_HYPHEN.matcher(normalized).replaceAll("-");

        // Trim leading and trailing hyphens
        if (normalized.startsWith("-")) {
            normalized = normalized.substring(1);
        }
        if (normalized.endsWith("-")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }

        return normalized;
    }
}
