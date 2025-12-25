package org.minbox.framework.message.pipe.server.manager;

import lombok.extern.slf4j.Slf4j;
import org.minbox.framework.message.pipe.core.Message;
import org.minbox.framework.message.pipe.server.config.MessagePipeConfiguration;
import org.redisson.api.RQueue;
import org.redisson.api.RedissonClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Dead letter queue manager for handling permanently failed messages
 *
 * @author 恒宇少年
 */
@Slf4j
public class MessageDeadLetterQueue {
    /**
     * Redis client instance
     */
    private final RedissonClient redissonClient;

    /**
     * Pipeline name (used to construct dead_letter queue name)
     */
    private final String pipeName;

    /**
     * MessagePipe configuration containing DLQ TTL settings
     */
    private final MessagePipeConfiguration configuration;

    /**
     * dead_letter queue name format: {pipeName}_dead_letter
     */
    private static final String DEAD_LETTER_NAME_FORMAT = "%s_dead_letter";

    /**
     * Constructor
     *
     * @param redissonClient the Redisson client
     * @param pipeName the message pipe name
     * @param configuration the message pipe configuration
     */
    public MessageDeadLetterQueue(RedissonClient redissonClient, String pipeName, MessagePipeConfiguration configuration) {
        this.redissonClient = redissonClient;
        this.pipeName = pipeName;
        this.configuration = configuration;
    }

    /**
     * Send a message to the dead letter queue
     * <p>
     * This is called when a message has exceeded maximum retry attempts
     * Message will automatically expire and be deleted after the configured TTL
     *
     * @param message the failed message
     * @param record the message retry record with retry information
     */
    public void send(Message message, MessageRetryRecord record) {
        String dlqName = getDeadLetterQueueName();
        RQueue<DeadLetterRecord> dlq = redissonClient.getQueue(dlqName, configuration.getCodec());

        DeadLetterRecord entry = DeadLetterRecord.of(
            message,
            record.getLastStatus() != null ? record.getLastStatus().toString() : "UNKNOWN",
            record.getRetryCount()
        );

        boolean offered = dlq.offer(entry);
        if (offered) {
            // Set TTL (Time To Live) for the DLQ queue
            // Messages will automatically expire and be deleted after the configured duration
            long expireSeconds = configuration.getDlqMessageExpireSeconds();
            try {
                dlq.expire(Duration.ofSeconds(expireSeconds));
                log.warn("Message moved to dead_letter [{}]: messageId={}, retryAttempts={}, expireSeconds={}",
                    dlqName, record.getMessageId(), record.getRetryCount(), expireSeconds);
            } catch (Exception e) {
                log.error("Failed to set TTL for dead_letter queue [{}]: messageId={}",
                    dlqName, record.getMessageId(), e);
            }
        } else {
            log.error("Failed to add message to dead_letter [{}]: messageId={}",
                dlqName, record.getMessageId());
        }
    }

    /**
     * Query all messages in the dead letter queue
     *
     * @return list of dead letter records
     */
    public List<DeadLetterRecord> listMessages() {
        String dlqName = getDeadLetterQueueName();
        RQueue<DeadLetterRecord> dlq = redissonClient.getQueue(dlqName, configuration.getCodec());

        try {
            return new ArrayList<>(dlq.readAll());
        } catch (Exception e) {
            log.error("Failed to query dead_letter [{}]", dlqName, e);
            return new ArrayList<>();
        }
    }

    /**
     * Get the count of messages in dead_letter
     *
     * @return number of messages in dead_letter
     */
    public int size() {
        String dlqName = getDeadLetterQueueName();
        RQueue<DeadLetterRecord> dlq = redissonClient.getQueue(dlqName, configuration.getCodec());

        try {
            return dlq.size();
        } catch (Exception e) {
            log.error("Failed to get dead_letter size [{}]", dlqName, e);
            return 0;
        }
    }

    /**
     * Remove a message from the dead letter queue
     * <p>
     * Used after manual inspection and decision (e.g., discard or recover)
     *
     * @param record the dead letter record to remove
     * @return true if removal was successful
     */
    public boolean remove(DeadLetterRecord record) {
        String dlqName = getDeadLetterQueueName();
        RQueue<DeadLetterRecord> dlq = redissonClient.getQueue(dlqName, configuration.getCodec());

        try {
            boolean removed = dlq.remove(record);
            if (removed) {
                log.info("Removed message from dead_letter [{}]: retryAttempts={}",
                    dlqName, record.getRetryAttempts());
            }
            return removed;
        } catch (Exception e) {
            log.error("Failed to remove message from dead_letter [{}]", dlqName, e);
            return false;
        }
    }

    /**
     * Clear all messages from the dead letter queue
     * <p>
     * Use with caution - this will delete all failed messages
     *
     * @return number of messages cleared
     */
    public int clear() {
        String dlqName = getDeadLetterQueueName();
        RQueue<DeadLetterRecord> dlq = redissonClient.getQueue(dlqName, configuration.getCodec());

        try {
            int size = dlq.size();
            dlq.clear();
            log.warn("Cleared dead_letter [{}]: {} messages deleted", dlqName, size);
            return size;
        } catch (Exception e) {
            log.error("Failed to clear dead_letter [{}]", dlqName, e);
            return 0;
        }
    }

    /**
     * Get dead_letter queue name for this pipe
     *
     * @return the dead_letter queue name
     */
    public String getDeadLetterName() {
        return getDeadLetterQueueName();
    }

    /**
     * Get the formatted dead letter queue name
     *
     * @return the dead_letter queue name
     */
    private String getDeadLetterQueueName() {
        return String.format(DEAD_LETTER_NAME_FORMAT, pipeName);
    }
}
