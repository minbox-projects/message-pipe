package org.minbox.framework.message.pipe.server.config;

import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.minbox.framework.message.pipe.core.converter.MessageConverter;
import org.minbox.framework.message.pipe.core.transport.RequestIdGenerator;
import org.minbox.framework.message.pipe.core.transport.RequestIdSequenceGenerator;
import org.minbox.framework.message.pipe.server.MessagePipe;
import org.minbox.framework.message.pipe.server.exception.ConsoleExceptionHandler;
import org.minbox.framework.message.pipe.server.exception.ExceptionHandler;
import org.minbox.framework.message.pipe.server.lb.ClientLoadBalanceStrategy;
import org.minbox.framework.message.pipe.server.lb.support.RandomWeightedStrategy;
import org.minbox.framework.message.pipe.server.manager.MessagePipeFactoryBean;
import org.redisson.client.codec.Codec;
import org.redisson.codec.JsonJacksonCodec;

import java.util.concurrent.TimeUnit;

/**
 * The message pipe configuration
 * <p>
 * Parameters for building message pipe
 *
 * @author 恒宇少年
 * @see MessagePipe
 * @see ExceptionHandler
 * @see MessageConverter
 * @see MessagePipeFactoryBean
 */
@Data
@Accessors(chain = true)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MessagePipeConfiguration {
    /**
     * Lock time configuration for "put" operation
     * <p>
     * Default leaseTime is 100 seconds to ensure sufficient time
     * for putting messages without losing the lock.
     * <p>
     * Default waitTime is 0 to fail fast if lock is busy.
     */
    private LockTime putLockTime = new LockTime().setLeaseTime(100).setWaitTime(0);
    /**
     * Lock time configuration for "take" operation
     * <p>
     * Default leaseTime is 300 seconds (5 minutes) to ensure sufficient time
     * for processing large batches (e.g., 100 messages) without losing the lock.
     * <p>
     * Default waitTime is 0 to fail fast if lock is busy, avoiding connection holding.
     */
    private LockTime takeLockTime = new LockTime().setLeaseTime(300).setWaitTime(0);
    /**
     * The batch size for processing messages
     * <p>
     * Used to reduce Redis interactions by pre-fetching messages.
     */
    private int batchSize = 100;
    /**
     * The batch size for putting messages
     * <p>
     * Used to split large batch writes into smaller chunks.
     */
    private int putBatchSize = 10;
    /**
     * The exception handler
     */
    private ExceptionHandler exceptionHandler = new ConsoleExceptionHandler();
    /**
     * The load client load-balance strategy
     */
    private ClientLoadBalanceStrategy loadBalanceStrategy = new RandomWeightedStrategy();
    /**
     * The requestId generator
     *
     * @see RequestIdSequenceGenerator
     */
    private RequestIdGenerator requestIdGenerator = new RequestIdSequenceGenerator();
    /**
     * The message request timeout millis
     * <p>
     * When the message distributor sends a message to the client,
     * the maximum waiting time for the request to complete
     * Default: 30000ms
     */
    private long messageRequestTimeoutMillis = 30000;
    /**
     * The interval time for each message pipeline to perform monitoring
     * time unit: milliseconds
     */
    private long messagePipeMonitorMillis = 1000L;
    /**
     * Configure the conversion method of redisson processing message content
     * <p>
     * The default {@link Codec} use {@link JsonJacksonCodec}
     */
    private Codec codec = new JsonJacksonCodec();
    /**
     * Dead letter queue (DLQ) TTL configuration
     * Default: 30 days (2592000 seconds)
     */
    private long dlqMessageExpireSeconds = 30 * 24 * 60 * 60;

    /**
     * Message retry record TTL configuration
     * <p>
     * Retry records stored in Redis will automatically expire after this duration.
     * Default: 30 days (2592000 seconds)
     */
    private long retryRecordExpireSeconds = 30 * 24 * 60 * 60;

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
         * wait get lock time
         */
        private long waitTime = 3;
        /**
         * lease time (milliseconds)
         */
        private long leaseTime = -1;
        /**
         * lease time unit
         *
         * @see TimeUnit
         */
        private TimeUnit timeUnit = TimeUnit.SECONDS;
    }
}
