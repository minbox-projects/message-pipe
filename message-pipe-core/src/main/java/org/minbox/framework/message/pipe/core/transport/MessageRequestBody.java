package org.minbox.framework.message.pipe.core.transport;

import lombok.Data;
import lombok.experimental.Accessors;
import org.minbox.framework.message.pipe.core.Message;

/**
 * The message request
 *
 * @author 恒宇少年
 */
@Data
@Accessors(chain = true)
public class MessageRequestBody {
    /**
     * Unique number of message request
     */
    private String requestId;
    /**
     * ID of the client receiving the message
     */
    private String clientId;
    /**
     * Only get messages in this pipe
     */
    private String pipeName;
    /**
     * The list of messages for batch processing
     */
    private java.util.List<Message> messages;
}
