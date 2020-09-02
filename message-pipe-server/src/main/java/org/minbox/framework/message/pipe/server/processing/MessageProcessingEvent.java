package org.minbox.framework.message.pipe.server.processing;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Message processing event
 *
 * @author 恒宇少年
 * @see MessageProcessingType
 * @see MessageProcessingListener
 */
@Getter
public class MessageProcessingEvent extends ApplicationEvent {
    /**
     * The name of message pipe
     */
    private String pipeName;
    private MessageProcessingType processingType;

    public MessageProcessingEvent(Object source, String pipeName, MessageProcessingType processingType) {
        super(source);
        this.pipeName = pipeName;
        this.processingType = processingType;
    }
}
