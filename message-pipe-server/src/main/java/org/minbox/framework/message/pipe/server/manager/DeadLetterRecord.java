package org.minbox.framework.message.pipe.server.manager;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.minbox.framework.message.pipe.core.Message;
import java.io.Serializable;

/**
 * Dead letter record - represents a message that failed after all retries
 * <p>
 * Stored in Redis dead letter queue when a message exhausts all retry attempts.
 * Contains message content, failure reason, and retry history.
 *
 * @author 恒宇少年
 */
@Getter
@Setter
@Accessors(chain = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeadLetterRecord implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * The failed message
     */
    private Message message;

    /**
     * Reason for failure (from MessageResponseStatus or exception message)
     */
    private String failureReason;

    /**
     * Number of retry attempts before moving to DLQ
     */
    private int retryAttempts;

    /**
     * Timestamp when message moved to DLQ (milliseconds)
     */
    private long failureTime;

    /**
     * Create a new dead letter record
     *
     * @param message the failed message
     * @param failureReason reason for failure
     * @param retryAttempts number of retry attempts
     * @return new DeadLetterRecord instance
     */
    public static DeadLetterRecord of(Message message, String failureReason, int retryAttempts) {
        return new DeadLetterRecord()
            .setMessage(message)
            .setFailureReason(failureReason)
            .setRetryAttempts(retryAttempts)
            .setFailureTime(System.currentTimeMillis());
    }
}
