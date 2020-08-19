package org.minbox.framework.message.pipe.server.exception;

import lombok.extern.slf4j.Slf4j;

/**
 * Console output stack information exception handler
 *
 * @author 恒宇少年
 */
@Slf4j
public class ConsoleExceptionHandler implements ExceptionHandler {
    @Override
    public void handleException(Exception exception, Object target) {
        log.error("Encountered once while processing [" + target + "] message.", exception);
    }
}
