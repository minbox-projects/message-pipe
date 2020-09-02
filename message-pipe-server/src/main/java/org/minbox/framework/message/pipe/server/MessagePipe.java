package org.minbox.framework.message.pipe.server;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.minbox.framework.message.pipe.core.Message;
import org.minbox.framework.message.pipe.core.exception.MessagePipeException;
import org.minbox.framework.message.pipe.server.config.MessagePipeConfiguration;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.util.concurrent.atomic.AtomicInteger;
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
    private String queueName;
    private AtomicInteger lastMessageCount = new AtomicInteger(0);
    private AtomicLong lastProcessTimeMillis = new AtomicLong(System.currentTimeMillis());
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
    @Getter
    private MessagePipeConfiguration configuration;

    public MessagePipe(String name,
                       RedissonClient redissonClient,
                       MessagePipeConfiguration configuration) {
        this.name = name;
        this.queueName = LockNames.MESSAGE_QUEUE.format(this.name);
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
                lastMessageCount.set(queue.size());
                if (!addSuccess) {
                    throw new MessagePipeException("Unsuccessful when writing the message to the queue.");
                }
            } catch (Exception e) {
                configuration.getExceptionHandler().handleException(e, message);
            } finally {
                putLock.unlock();
            }
        }
    }

    /**
     * Lock processing the first message
     *
     * @param function Logical method of processing messages
     */
    public void lockHandleTheFirst(Function<Message, Boolean> function) {
        Message message = null;
        String takeLockName = LockNames.TAKE_MESSAGE.format(this.name);
        RLock takeLock = redissonClient.getLock(takeLockName);
        log.debug("lock:" + takeLock.toString() + ",interrupted:" + Thread.currentThread().isInterrupted()
                + ",hold:" + takeLock.isHeldByCurrentThread() + ",threadId:" + Thread.currentThread().getId());
        try {
            MessagePipeConfiguration.LockTime lockTime = configuration.getLockTime();
            if (takeLock.tryLock(lockTime.getWaitTime(), lockTime.getLeaseTime(), lockTime.getTimeUnit())) {
                log.debug("Thread：{}, acquired lock.", Thread.currentThread().getId());
                RBlockingQueue<Message> queue = redissonClient.getBlockingQueue(this.queueName);
                this.lastMessageCount.set(queue.size());
                message = queue.peek();
                boolean isExecutionSuccessfully = message != null ? function.apply(message) : false;
                if (isExecutionSuccessfully) {
                    Long currentTimeMillis = System.currentTimeMillis();
                    this.lastProcessTimeMillis.set(currentTimeMillis);
                    queue.poll();
                }
            }
        } catch (Exception e) {
            configuration.getExceptionHandler().handleException(e, message);
        } finally {
            if (!this.checkClientIsShutdown() && takeLock.isLocked() && takeLock.isHeldByCurrentThread()) {
                takeLock.unlock();
            }
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
            RBlockingQueue<Message> queue = redissonClient.getBlockingQueue(queueName);
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
            RBlockingQueue<Message> queue = redissonClient.getBlockingQueue(queueName);
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
            RBlockingQueue<Message> queue = redissonClient.getBlockingQueue(queueName);
            messageSize = queue.size();
            this.lastMessageCount.set(messageSize);
        }
        return messageSize;
    }

    /**
     * Get last invoke {@link #lockHandleTheFirst} method time millis
     *
     * @return Last call time，{@link java.util.concurrent.TimeUnit#MILLISECONDS}
     */
    public Long getLastProcessTimeMillis() {
        return this.lastProcessTimeMillis.get();
    }

    /**
     *
     * @return
     */
    public int getLastMessageCount() {
        return this.lastMessageCount.get();
    }

    /**
     * Check whether the redisson client has been shutdown
     *
     * @return When it returns true, it means it has been shutdown
     */
    private boolean checkClientIsShutdown() {
        return redissonClient.isShutdown() || redissonClient.isShuttingDown();
    }

}
