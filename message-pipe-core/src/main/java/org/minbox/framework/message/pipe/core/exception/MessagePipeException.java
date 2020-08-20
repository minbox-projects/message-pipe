package org.minbox.framework.message.pipe.core.exception;

import lombok.NoArgsConstructor;

/**
 * Handle exceptions encountered by messages in the pipeline
 *
 * @author 恒宇少年
 */
@NoArgsConstructor
public class MessagePipeException extends RuntimeException {
    public MessagePipeException(String message) {
        super(message);
    }

    public MessagePipeException(String message, Throwable cause) {
        super(message, cause);
    }
}
