package org.minbox.framework.message.pipe.server.processing.pop;

import lombok.Getter;
import org.minbox.framework.message.pipe.server.MessagePipe;
import org.springframework.context.ApplicationEvent;

/**
 * @author 恒宇少年
 */
@Getter
public class PopMessageEvent extends ApplicationEvent {
    /**
     * The name of {@link MessagePipe}
     */
    private String pipeName;

    public PopMessageEvent(Object source, String pipeName) {
        super(source);
        this.pipeName = pipeName;
    }
}
