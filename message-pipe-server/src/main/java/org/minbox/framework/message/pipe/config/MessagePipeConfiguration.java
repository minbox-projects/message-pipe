package org.minbox.framework.message.pipe.config;

import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.minbox.framework.message.pipe.core.Message;
import org.minbox.framework.message.pipe.core.converter.MessageConverter;
import org.minbox.framework.message.pipe.exception.ConsoleExceptionHandler;
import org.minbox.framework.message.pipe.exception.ExceptionHandler;

import java.util.concurrent.TimeUnit;

/**
 * The message pipe configuration
 * <p>
 * Parameters for building message pipe
 *
 * @author 恒宇少年
 * @see org.minbox.framework.message.pipe.MessagePipe
 * @see ExceptionHandler
 * @see MessageConverter
 * @see org.minbox.framework.message.pipe.MessagePipeFactoryBean
 */
@Data
@Accessors(chain = true)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MessagePipeConfiguration {
    /**
     * Lock time configuration when processing messages
     */
    private LockTime lockTime;
    /**
     * The Exception handler
     */
    private ExceptionHandler exceptionHandler = new ConsoleExceptionHandler();
    /**
     * The {@link Message} converter
     */
    private MessageConverter converter;

    /**
     * Get the default {@link MessagePipeConfiguration}
     *
     * @return {@link MessagePipeConfiguration} instance
     */
    public static MessagePipeConfiguration defaultConfiguration() {
        return new MessagePipeConfiguration();
    }

    /**
     * Lock related information when configuring channel message distribution
     */
    @Data
    @Accessors(chain = true)
    public static class LockTime {
        /**
         * lease time
         */
        private long leaseTime;
        /**
         * lease time unit
         *
         * @see TimeUnit
         */
        private TimeUnit timeUnit;
    }
}
