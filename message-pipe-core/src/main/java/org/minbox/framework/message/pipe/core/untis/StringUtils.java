package org.minbox.framework.message.pipe.core.untis;

/**
 * string utils
 *
 * @author 恒宇少年
 */
public class StringUtils {
    /**
     * Check if the string is empty
     *
     * @param value check value
     * @return Return "true" when empty
     */
    public static boolean isEmpty(String value) {
        return value == null || value.trim().length() == 0;
    }
}
