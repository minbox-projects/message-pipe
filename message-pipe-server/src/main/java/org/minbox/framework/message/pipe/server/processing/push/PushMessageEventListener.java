package org.minbox.framework.message.pipe.server.processing.push;

import lombok.extern.slf4j.Slf4j;
import org.minbox.framework.message.pipe.server.manager.MessagePipeManager;
import org.springframework.context.ApplicationListener;

/**
 * Listener for {@link PushMessageEvent}
 * <p>
 * When a message is pushed to the pipe (detected via Redis event), trigger processing.
 *
 * @author 恒宇少年
 */
@Slf4j
public class PushMessageEventListener implements ApplicationListener<PushMessageEvent> {

    private final MessagePipeManager messagePipeManager;

    public PushMessageEventListener(MessagePipeManager messagePipeManager) {
        this.messagePipeManager = messagePipeManager;
    }

    @Override
    public void onApplicationEvent(PushMessageEvent event) {
        String pipeName = event.getPipeName();
        log.debug("Received PushMessageEvent for pipe: {}", pipeName);
        messagePipeManager.process(pipeName);
    }
}
