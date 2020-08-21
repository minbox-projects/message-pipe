package org.minbox.framework.message.pipe.server.config;

import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.minbox.framework.message.pipe.core.converter.MessageConverter;
import org.minbox.framework.message.pipe.server.MessagePipe;
import org.minbox.framework.message.pipe.server.MessagePipeFactoryBean;
import org.minbox.framework.message.pipe.server.distribution.MessageDistributionExecutor;
import org.minbox.framework.message.pipe.server.distribution.RequestIdSequenceGenerator;
import org.minbox.framework.message.pipe.server.distribution.RequestIdGenerator;
import org.minbox.framework.message.pipe.server.exception.ConsoleExceptionHandler;
import org.minbox.framework.message.pipe.server.exception.ExceptionHandler;
import org.minbox.framework.message.pipe.server.lb.ClientLoadBalanceStrategy;
import org.minbox.framework.message.pipe.server.lb.support.RandomWeightedStrategy;

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
     * Lock time configuration when processing messages
     */
    private LockTime lockTime;
    /**
     * The Exception handler
     *
     * @see MessagePipe
     * @see MessageDistributionExecutor
     */
    private ExceptionHandler exceptionHandler = new ConsoleExceptionHandler();
    /**
     * The load client load-balance strategy
     *
     * @see MessageDistributionExecutor
     */
    private ClientLoadBalanceStrategy loadBalanceStrategy = new RandomWeightedStrategy();
    /**
     * The requestId generator
     *
     * @see RequestIdSequenceGenerator
     */
    private RequestIdGenerator requestIdGenerator = new RequestIdSequenceGenerator();
    /**
     * The number of thread pool threads for message channel distribution
     *
     * @see MessageDistributionExecutor
     */
    private int distributionMessagePoolSize = 5;

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
