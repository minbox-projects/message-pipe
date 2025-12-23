package org.minbox.framework.message.pipe.server.processing.pop;

import lombok.Getter;
import org.minbox.framework.message.pipe.server.MessagePipe;
import org.springframework.context.ApplicationEvent;

/**
 * Event published when a message is popped from a message pipe
 *
 * @author 恒宇少年
 */
@Getter
public class PopMessageEvent extends ApplicationEvent {
    /**
     * The name of {@link MessagePipe}
     */
    private String pipeName;

    /**
     * Constructs a new PopMessageEvent instance
     *
     * @param source the event source
     * @param pipeName the name of the message pipe
     */
    public PopMessageEvent(Object source, String pipeName) {
        super(source);
        this.pipeName = pipeName;
    }
}
