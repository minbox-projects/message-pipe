package org.minbox.framework.message.pipe.server.processing.push;

import lombok.Getter;
import org.minbox.framework.message.pipe.server.MessagePipe;
import org.springframework.context.ApplicationEvent;

/**
 * Push a message to pipe event
 * <p>
 * This event is triggered when a message is added to the pipeline
 *
 * @author 恒宇少年
 */
@Getter
public class PushMessageEvent extends ApplicationEvent {
    /**
     * The name of {@link MessagePipe}
     */
    private String pipeName;

    public PushMessageEvent(Object source, String pipeName) {
        super(source);
        this.pipeName = pipeName;
    }
}
