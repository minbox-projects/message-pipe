package org.minbox.framework.message.pipe.core.transport;

import lombok.Data;
import lombok.experimental.Accessors;

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
     * The name of message pipe
     * <p>
     * Only get messages in this pipe
     */
    private String pipeName;
}
