package org.minbox.framework.message.pipe.config;

import lombok.Data;
import lombok.experimental.Accessors;
import org.minbox.framework.message.pipe.core.converter.MessageConverter;
import org.minbox.framework.message.pipe.exception.ExceptionHandler;
import org.minbox.framework.message.pipe.exception.MessagePipeException;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.minbox.framework.message.pipe.core.Message;

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
     * The {@link Message} converter
     */
    private MessageConverter converter;
    /**
     * The Redisson client instance
     * <p>
     * Used to handle redis distributed locks and blocking queues
     */
    private RedissonClient redissonClient;

    /**
     * Instantiate the message pipe with the specified name
     * <p>
     * The {@link #name}、{@link #redissonClient} it's required
     *
     * @param name           The message pipe name
     * @param redissonClient The redissonclient instance
     */
    public MessagePipeConfiguration(String name, RedissonClient redissonClient) {
        this.name = name;
        if (this.name == null || this.name.trim().length() == 0) {
            throw new MessagePipeException("The MessagePipe name is required，cannot be empty.");
        }
        this.redissonClient = redissonClient;
        if (this.redissonClient == null) {
            throw new MessagePipeException("The RedissonClient instance cannot be null.");
        }
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
