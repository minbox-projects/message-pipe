package org.minbox.framework.message.pipe.server;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.minbox.framework.message.pipe.core.Message;
import org.minbox.framework.message.pipe.core.exception.MessagePipeException;
import org.minbox.framework.message.pipe.server.config.LockNames;
import org.minbox.framework.message.pipe.server.config.MessagePipeConfiguration;
import org.minbox.framework.message.pipe.server.exception.ExceptionHandler;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.util.ObjectUtils;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

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
     * The Redis blocking queue bound to the current message pipeline
     */
    @Getter
    private RBlockingQueue<Message> queue;
    /**
     * The redisson client instance
     *
     * @see RBlockingQueue
     * @see RLock
     */
    @Getter
    private RedissonClient redissonClient;
    /**
     * The queue name in redis
     */
    private String queueName;
    /**
     * The name of the lock used when putting the message
     */
    private String putLockName;
    /**
     * The lock name used when taking the message
     */
    private String takeLockName;
    /**
     * The last processing message millis
     * <p>
     * The default values is {@link System#currentTimeMillis()}
     */
    private AtomicLong lastProcessTimeMillis = new AtomicLong(System.currentTimeMillis());
    /**
     * Whether the message monitoring method is being executed
     */
    private boolean runningHandleAll = false;
    /**
     * Is the add data method being executed
     */
    private boolean transfer = false;
    /**
     * The {@link MessagePipe} configuration
     */
    @Getter
    private MessagePipeConfiguration configuration;

    public MessagePipe(String name,
                       RedissonClient redissonClient,
                       MessagePipeConfiguration configuration) {
        this.name = name;
        this.queueName = LockNames.MESSAGE_QUEUE.format(this.name);
        this.putLockName = LockNames.PUT_MESSAGE.format(this.name);
        this.takeLockName = LockNames.TAKE_MESSAGE.format(this.name);
        this.redissonClient = redissonClient;
        this.queue = redissonClient.getBlockingQueue(this.queueName);
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
    }

    /**
     * put message to current {@link MessagePipe}
     *
     * @param message The {@link Message} instance
     */
    public synchronized void putLast(Message message) {
        this.transfer = true;
        RLock putLock = redissonClient.getLock(putLockName);
        try {
            MessagePipeConfiguration.LockTime lockTime = configuration.getLockTime();
            if (putLock.tryLock(lockTime.getWaitTime(), lockTime.getLeaseTime(), lockTime.getTimeUnit())) {
                boolean addSuccess = queue.offer(message);
                if (!addSuccess) {
                    throw new MessagePipeException("Unsuccessful when writing the message to the queue.");
                }
            }
        } catch (Exception e) {
            this.doHandleException(e, message);
        } finally {
            this.transfer = false;
            putLock.unlock();
            notifyAll();
        }
    }

    /**
     * Processing first message
     *
     * @param function Logical method of processing first message in {@link MessagePipe}
     */
    public synchronized void handleFirst(Function<Message, Boolean> function) {
        while (transfer || runningHandleAll) {
            try {
                wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error(e.getMessage(), e);
            }
        }
        Message current = null;
        Long currentTimeMillis = System.currentTimeMillis();
        RLock takeLock = redissonClient.getLock(takeLockName);
        try {
            MessagePipeConfiguration.LockTime lockTime = configuration.getLockTime();
            if (takeLock.tryLock(lockTime.getWaitTime(), lockTime.getLeaseTime(), lockTime.getTimeUnit())) {
                // Take first message
                current = this.peek();
                if (ObjectUtils.isEmpty(current)) {
                    log.warn("Message pipeline: {}, no message to be processed was found.", name);
                    return;
                }
                boolean executionResult = function.apply(current);
                if (!executionResult) {
                    throw new MessagePipeException("MessagePipe [" + name + "] , Handle message exception, message content: " +
                            new String(current.getBody()));
                }
                // Remove first message
                this.poll();
            }
        } catch (Exception e) {
            this.doHandleException(e, current);
        } finally {
            lastProcessTimeMillis.set(currentTimeMillis);
            transfer = true;
            takeLock.unlock();
            notifyAll();
        }
    }

    /**
     * Process messages sequentially until all processing is complete
     *
     * @param function Logical method of processing messages in a loop
     */
    public synchronized void handleToLast(Function<Message, Boolean> function) {
        runningHandleAll = true;
        RLock takeLock = redissonClient.getLock(takeLockName);
        Message current = null;
        try {
            MessagePipeConfiguration.LockTime lockTime = configuration.getLockTime();
            if (takeLock.tryLock(lockTime.getWaitTime(), lockTime.getLeaseTime(), lockTime.getTimeUnit())) {
                while (queue.size() > 0) {
                    // Take first message
                    current = this.peek();
                    boolean executionResult = function.apply(current);
                    if (!executionResult) {
                        throw new MessagePipeException("Handle message exception, message content: " +
                                new String(current.getBody()));
                    }
                    // Remove first message
                    this.poll();
                }
            }
        } catch (Exception e) {
            this.doHandleException(e, current);
        } finally {
            transfer = true;
            runningHandleAll = false;
            takeLock.unlock();
            notifyAll();
        }
    }

    /**
     * Retrieves, but does not remove, the head of this queue,
     * or returns {@code null} if this queue is empty.
     *
     * @return the head of this queue, or {@code null} if this queue is empty
     */
    public Message peek() {
        Message message = null;
        if (!this.checkClientIsShutdown()) {
            message = queue.peek();
        }
        return message;
    }

    /**
     * Retrieves and removes the head of this queue,
     * or returns {@code null} if this queue is empty.
     *
     * @return the head of this queue, or {@code null} if this queue is empty
     */
    public Message poll() {
        Message message = null;
        if (!this.checkClientIsShutdown()) {
            message = queue.poll();
        }
        return message;
    }

    /**
     * Get the number of current messages in the pipeline
     *
     * @return count of message
     */
    public int size() {
        int messageSize = 0;
        if (!this.checkClientIsShutdown()) {
            messageSize = this.queue.size();
        }
        return messageSize;
    }

    /**
     * Get last invoke {@link #handleFirst}、{@link #handleToLast} method time millis
     *
     * @return Last call time，{@link java.util.concurrent.TimeUnit#MILLISECONDS}
     */
    public Long getLastProcessTimeMillis() {
        return this.lastProcessTimeMillis.get();
    }

    /**
     * Check whether the redisson client has been shutdown
     *
     * @return When it returns true, it means it has been shutdown
     */
    private boolean checkClientIsShutdown() {
        return redissonClient.isShutdown() || redissonClient.isShuttingDown();
    }

    /**
     * Execution processing exception
     *
     * @param e       The {@link Exception} instance
     * @param current {@link Message} instance being processed
     */
    private void doHandleException(Exception e, Message current) {
        if (e instanceof InterruptedException) {
            Thread.currentThread().interrupt();
        }
        ExceptionHandler exceptionHandler = configuration.getExceptionHandler();
        exceptionHandler.handleException(e, current);
    }
}
