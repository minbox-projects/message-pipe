package org.minbox.framework.message.pipe.core.transport;

import lombok.Data;
import lombok.experimental.Accessors;
import org.minbox.framework.message.pipe.core.Message;

/**
 * The message response
 *
 * @author 恒宇少年
 */
@Data
@Accessors(chain = true)
public class MessageResponseBody {
    /**
     * Message request number processed
     */
    private String requestId;
    /**
     * Message processing response status
     * <p>
     * If the response status is {@link MessageResponseStatus#SUCCESS},continue to execute downward.
     * if it is {@link MessageResponseStatus#ERROR},
     * need to execute message distribution again according to the retry strategy
     */
    private MessageResponseStatus status;
    /**
     * The number of messages successfully processed in a batch
     */
    private int successCount = 0;
}
