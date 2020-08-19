package org.minbox.framework.message.pipe.server;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.minbox.framework.message.pipe.core.ClientInformation;
import org.minbox.framework.message.pipe.server.config.MessagePipeConfiguration;
import org.minbox.framework.message.pipe.core.Message;
import org.minbox.framework.message.pipe.server.distribution.MessageDistributionExecutor;
import org.minbox.framework.message.pipe.server.exception.ExceptionHandler;
import org.minbox.framework.message.pipe.server.exception.MessagePipeException;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * The message pipe
 *
 * @author 恒宇少年
 */
@Slf4j
public class MessagePipe {
    /**
     * Name of current message pipe
     * <p>
     * this name is used to create the {@link RBlockingQueue} and {@link RLock}
     * the format is:#name.queues"、"#name.write.lock"、"#name.read.lock"
     */
    @Getter
    private String name;
    /**
     * The redisson client instance
     *
     * @see RBlockingQueue
     * @see RLock
     */
    private RedissonClient redissonClient;
    /**
     * The {@link MessagePipe} configuration
     */
    private MessagePipeConfiguration configuration;
    /**
     * The exception handler
     */
    private ExceptionHandler exceptionHandler;

    public MessagePipe(String name, RedissonClient redissonClient, MessagePipeConfiguration configuration) {
        this.name = name;
        this.redissonClient = redissonClient;
        this.configuration = configuration;
        if (this.name == null || this.name.trim().length() == 0) {
            throw new MessagePipeException("The MessagePipe name is required，cannot be null.");
        }
        if (this.redissonClient == null) {
            throw new MessagePipeException("The RedissonClient cannot be null.");
        }
        if (this.configuration == null) {
            throw new MessagePipeException("The MessagePipeConfiguration cannot be null.");
        }
        this.exceptionHandler = configuration.getExceptionHandler();
        // Start waiting dual new message
        MessageDistributionExecutor messageDistributionExecutor = new MessageDistributionExecutor(this.name,
                this.redissonClient, this.configuration);
        messageDistributionExecutor.waitingForNewMessage();
    }

    /**
     * put message to current {@link MessagePipe}
     *
     * @param message The {@link Message} instance
     */
    public void put(Message message) {
        String putLockName = LockNames.PUT_MESSAGE.format(this.name);
        RLock putLock = redissonClient.getLock(putLockName);
        putLock.lock();
        if (!Thread.currentThread().isInterrupted()) {
            try {
                String queueLockName = LockNames.MESSAGE_QUEUE.format(this.name);
                RBlockingQueue<Message> queue = redissonClient.getBlockingQueue(queueLockName);
                boolean addSuccess = queue.offer(message);
                if (!addSuccess) {
                    throw new MessagePipeException("Unsuccessful when writing the message to the queue.");
                }
            } catch (Exception e) {
                this.exceptionHandler.handleException(e, message);
            } finally {
                putLock.unlock();
            }
        }
    }
}
