package org.minbox.framework.message.pipe.core.untis;

import java.util.regex.Pattern;

/**
 * Regular expression utility class
 *
 * @author 恒宇少年
 */
public class RegexUtils {

    /**
     * Check if the value matches the pattern
     * <p>
     * Supports standard regex and simple wildcard '*' (converted to '.*')
     *
     * @param pattern The regex pattern (e.g. "pipe-.*" or "pipe-*")
     * @param value   The value to check (e.g. "pipe-1")
     * @return true if matches
     */
    public static boolean isMatch(String pattern, String value) {
        if (pattern == null || value == null) {
            return false;
        }
        
        boolean isMatch = false;
        try {
            isMatch = Pattern.compile(pattern).matcher(value).matches();
        } catch (Exception e) {
            // Ignore compilation errors for raw patterns
        }

        // If strict regex match fails, try wildcard compatibility (treat * as .*) 
        if (!isMatch && pattern.contains("*")) {
            try {
                String wildcardPattern = pattern.replaceAll("\\*", ".*");
                isMatch = Pattern.compile(wildcardPattern).matcher(value).matches();
            } catch (Exception e) {
                // Ignore fallback compilation errors
            }
        }
        return isMatch;
    }
}
