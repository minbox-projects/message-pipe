package org.minbox.framework.message.pipe.server.exception;

import lombok.extern.slf4j.Slf4j;
import org.minbox.framework.message.pipe.core.Message;
import org.minbox.framework.message.pipe.server.manager.MessageProcessStatus;
import org.springframework.util.ObjectUtils;

/**
 * Console output stack information exception handler
 *
 * @author 恒宇少年
 */
@Slf4j
public class ConsoleExceptionHandler implements ExceptionHandler {
    @Override
    public void handleException(Exception exception, MessageProcessStatus status, Message message) {
        if (!ObjectUtils.isEmpty(message)) {
            log.error("Encountered once while processing [" + new String(message.getBody()) + "] message.", exception);
        }
    }
}
