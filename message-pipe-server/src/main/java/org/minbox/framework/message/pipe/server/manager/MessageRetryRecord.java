package org.minbox.framework.message.pipe.server.manager;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.minbox.framework.message.pipe.core.Message;
import org.minbox.framework.message.pipe.core.transport.MessageResponseStatus;

/**
 * Message retry record for tracking retry attempts and failure status
 * <p>
 * Created only when message sending fails (SEND_EXCEPTION status).
 * Tracks retry count, delays, and failure information for messages that need to be retried.
 *
 * @author 恒宇少年
 */
@Getter
@Setter
@Accessors(chain = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MessageRetryRecord {
    /**
     * Message ID for correlation
     */
    private String messageId;

    /**
     * The message content
     */
    private Message message;

    /**
     * Current retry count
     */
    private int retryCount = 0;

    /**
     * Maximum retry attempts (default: 3)
     * <p>
     * Configuration: after reaching maxRetries, message will be moved to DLQ
     */
    private int maxRetries = 3;

    /**
     * First failure timestamp (milliseconds)
     */
    private long firstFailureTime;

    /**
     * Last retry timestamp (milliseconds)
     */
    private long lastRetryTime;

    /**
     * Last response status from client
     */
    private MessageResponseStatus lastStatus;

    /**
     * Determine if should retry based on retry count
     *
     * @return true if retryCount < maxRetries
     */
    public boolean shouldRetry() {
        return retryCount < maxRetries;
    }

    /**
     * Calculate retry delay with exponential backoff
     * <p>
     * Delay formula: 1s * 2^retryCount
     * - 1st retry: 1 second
     * - 2nd retry: 2 seconds
     * - 3rd retry: 4 seconds
     * - 4th retry: 8 seconds
     *
     * @return delay in milliseconds
     */
    public long getRetryDelayMillis() {
        return 1000L * (long) Math.pow(2, retryCount);
    }

    /**
     * Create a new retry record for a message
     *
     * @param messageId the message ID (unique identifier)
     * @param message the message content
     * @return new MessageRetryRecord instance with initialized fields
     */
    public static MessageRetryRecord of(String messageId, Message message) {
        return new MessageRetryRecord()
            .setMessageId(messageId)
            .setMessage(message)
            .setRetryCount(0)
            .setFirstFailureTime(System.currentTimeMillis());
    }
}
