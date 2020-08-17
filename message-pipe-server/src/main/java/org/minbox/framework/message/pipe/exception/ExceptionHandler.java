package org.minbox.framework.message.pipe.exception;

/**
 * The Exception Handler
 * <p>
 * Customize business processing after encountering exceptions
 *
 * @author 恒宇少年
 */
@FunctionalInterface
public interface ExceptionHandler {
    /**
     * Handle exceptions encountered when reading pipeline messages
     *
     * @param exception The {@link Exception} instance
     * @param target    Value being processed
     */
    void handleException(Exception exception, Object target);
}
