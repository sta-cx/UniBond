package com.unibond.common.util;

import java.util.regex.Pattern;

public final class InputSanitizer {
    private static final Pattern XSS_PATTERN = Pattern.compile(
        "<[^>]*>|javascript:|on\\w+\\s*=", Pattern.CASE_INSENSITIVE);
    private static final Pattern EMOJI_PATTERN = Pattern.compile(
        "[\\p{So}\\p{Sk}\\p{Sc}\\x{1F600}-\\x{1F64F}\\x{1F300}-\\x{1F5FF}" +
        "\\x{1F680}-\\x{1F6FF}\\x{1F900}-\\x{1F9FF}\\x{2600}-\\x{26FF}\\x{2700}-\\x{27BF}]+");

    private InputSanitizer() {}

    /** Strip HTML tags and XSS patterns, then truncate to maxLen */
    public static String sanitizeText(String input, int maxLen) {
        if (input == null) return null;
        String clean = XSS_PATTERN.matcher(input).replaceAll("");
        return clean.length() > maxLen ? clean.substring(0, maxLen) : clean;
    }

    /** Validate that input contains only emoji characters */
    public static boolean isValidEmoji(String input) {
        if (input == null || input.isBlank()) return false;
        return EMOJI_PATTERN.matcher(input).matches();
    }
}
