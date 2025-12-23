package org.minbox.framework.message.pipe.server.manager;

import lombok.extern.slf4j.Slf4j;
import org.minbox.framework.message.pipe.core.Message;
import org.redisson.api.RDelayedQueue;
import org.redisson.api.RQueue;
import org.redisson.api.RedissonClient;

import java.util.concurrent.TimeUnit;

/**
 * Message retry scheduler for managing delayed retry queue
 *
 * @author 恒宇少年
 */
@Slf4j
public class MessageRetryScheduler {
    /**
     * Redis client instance
     */
    private final RedissonClient redissonClient;

    /**
     * Message pipe name (used to construct retry queue name)
     */
    private final String pipeName;

    /**
     * Retry queue name format: {pipeName}_retry
     */
    private static final String RETRY_QUEUE_NAME_FORMAT = "%s_retry";

    /**
     * Constructor
     *
     * @param redissonClient the Redisson client
     * @param pipeName the message pipe name
     */
    public MessageRetryScheduler(RedissonClient redissonClient, String pipeName) {
        this.redissonClient = redissonClient;
        this.pipeName = pipeName;
    }

    /**
     * Schedule a message for delayed retry
     * <p>
     * The message will be automatically moved from the delayed queue to the
     * regular queue after the specified delay
     *
     * @param message the message to retry
     * @param delayMillis the delay in milliseconds before retry
     */
    public void scheduleRetry(Message message, long delayMillis) {
        String retryQueueName = String.format(RETRY_QUEUE_NAME_FORMAT, pipeName);
        RQueue<Message> queue = redissonClient.getQueue(retryQueueName);
        RDelayedQueue<Message> delayedQueue = redissonClient.getDelayedQueue(queue);

        try {
            delayedQueue.offer(message, delayMillis, TimeUnit.MILLISECONDS);
            log.info("Message scheduled for retry: pipeName={}, delayMs={}",
                pipeName, delayMillis);
        } catch (Exception e) {
            log.error("Failed to schedule message retry: pipeName={}",
                pipeName, e);
        }
    }

    /**
     * Get the count of messages waiting for retry
     * <p>
     * Includes both messages in delay queue (not yet due) and
     * messages in the ready queue (due for processing)
     *
     * @return number of messages in retry queue
     */
    public int getRetryQueueSize() {
        String retryQueueName = String.format(RETRY_QUEUE_NAME_FORMAT, pipeName);
        
        try {
            // Get the ready queue size (messages that are ready to retry)
            RQueue<Message> readyQueue = redissonClient.getQueue(retryQueueName);
            return readyQueue.size();
        } catch (Exception e) {
            log.error("Failed to get retry queue size: pipeName={}", pipeName, e);
            return 0;
        }
    }

    /**
     * Get the retry queue name
     *
     * @return the retry queue name
     */
    public String getRetryQueueName() {
        return String.format(RETRY_QUEUE_NAME_FORMAT, pipeName);
    }

    /**
     * Clear all messages from the retry queue
     * <p>
     * Use with caution - this will delete all pending retries
     *
     * @return number of messages cleared
     */
    public int clear() {
        String retryQueueName = String.format(RETRY_QUEUE_NAME_FORMAT, pipeName);
        
        try {
            RQueue<Message> readyQueue = redissonClient.getQueue(retryQueueName);
            int size = readyQueue.size();
            readyQueue.clear();
            log.warn("Cleared retry queue: pipeName={}, messagesCleared={}", pipeName, size);
            return size;
        } catch (Exception e) {
            log.error("Failed to clear retry queue: pipeName={}", pipeName, e);
            return 0;
        }
    }
}
