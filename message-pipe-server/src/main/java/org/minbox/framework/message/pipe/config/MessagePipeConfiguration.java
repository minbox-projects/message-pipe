package org.minbox.framework.message.pipe.config;

import lombok.Data;
import lombok.experimental.Accessors;
import org.minbox.framework.message.pipe.core.ExceptionHandler;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RLock;

import java.util.concurrent.TimeUnit;

/**
 * The message pipe configuration
 * <p>
 * Parameters for building message pipe
 *
 * @author 恒宇少年
 * @see org.minbox.framework.message.pipe.MessagePipe
 * @see org.minbox.framework.message.pipe.MessagePipeFactoryBean
 */
@Data
@Accessors(chain = true)
public class MessagePipeConfiguration {
    /**
     * Name of current message pipe
     * <p>
     * this name is used to create the {@link RBlockingQueue} and {@link RLock}
     * the format is:#name.queues"、"#name.write.lock"、"#name.read.lock"
     */
    private String name;
    /**
     * Lock time configuration when processing messages
     */
    private LockTime lockTime;
    /**
     * The Exception handler
     */
    private ExceptionHandler exceptionHandler;

    /**
     * Instantiate the message pipe with the specified name
     *
     * @param name The message pipe name
     */
    public MessagePipeConfiguration(String name) {
        this.name = name;
    }

    /**
     * Instantiate the message pipeline with the specified name and configurable exception
     *
     * @param name             The message pipe name
     * @param exceptionHandler Custom handling exception
     */
    public MessagePipeConfiguration(String name, ExceptionHandler exceptionHandler) {
        this(name);
        this.exceptionHandler = exceptionHandler;
    }

    /**
     * Specify the distributed lock holding time configuration and exception handling to instantiate the message pipeline
     *
     * @param name             The message pipe name
     * @param lockTime         lock time
     * @param exceptionHandler Custom handling exception
     */
    public MessagePipeConfiguration(String name, LockTime lockTime, ExceptionHandler exceptionHandler) {
        this(name, exceptionHandler);
        this.lockTime = lockTime;
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
