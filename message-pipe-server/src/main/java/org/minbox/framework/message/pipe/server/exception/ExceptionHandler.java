package org.minbox.framework.message.pipe.server.exception;

import org.minbox.framework.message.pipe.core.Message;
import org.minbox.framework.message.pipe.server.manager.MessageProcessStatus;

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
     * @param status    send status
     * @param message   Send an exception message instance
     */
    void handleException(Exception exception, MessageProcessStatus status, Message message);
}
