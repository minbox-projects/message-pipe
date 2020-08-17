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
public class MessageRequest {
    /**
     * Unique number of message request
     */
    private String requestId;
    /**
     * The message subject of this consumption
     */
    private Message message;
}
