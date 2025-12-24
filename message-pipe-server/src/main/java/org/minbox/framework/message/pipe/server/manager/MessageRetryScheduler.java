package org.minbox.framework.message.pipe.server.manager;

import lombok.extern.slf4j.Slf4j;
import org.minbox.framework.message.pipe.core.Message;
import org.minbox.framework.message.pipe.server.config.LockNames;
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
        String queueName = LockNames.MESSAGE_QUEUE.format(pipeName);
        RQueue<Message> queue = redissonClient.getQueue(queueName);
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
        String queueName = LockNames.MESSAGE_QUEUE.format(pipeName);

        try {
            // Get both the ready queue size and delayed queue size
            RQueue<Message> readyQueue = redissonClient.getQueue(queueName);
            RDelayedQueue<Message> delayedQueue = redissonClient.getDelayedQueue(readyQueue);

            return delayedQueue.size();
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
        return LockNames.MESSAGE_QUEUE.format(pipeName);
    }

    /**
     * Clear all messages from the retry queue
     * <p>
     * Use with caution - this will delete all pending retries
     *
     * @return number of messages cleared
     */
    public int clear() {
        String queueName = LockNames.MESSAGE_QUEUE.format(pipeName);
        
        try {
            RQueue<Message> readyQueue = redissonClient.getQueue(queueName);
            RDelayedQueue<Message> delayedQueue = redissonClient.getDelayedQueue(readyQueue);
            int size = delayedQueue.size();
            delayedQueue.clear();
            log.warn("Cleared retry queue: pipeName={}, messagesCleared={}", pipeName, size);
            return size;
        } catch (Exception e) {
            log.error("Failed to clear retry queue: pipeName={}", pipeName, e);
            return 0;
        }
    }
}
