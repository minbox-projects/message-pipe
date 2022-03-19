package org.minbox.framework.message.pipe.server.manager;

import lombok.extern.slf4j.Slf4j;
import org.minbox.framework.message.pipe.core.exception.MessagePipeException;
import org.minbox.framework.message.pipe.server.MessagePipe;
import org.springframework.util.Assert;

/**
 * Message scheduling class in message pipeline
 *
 * @author 恒宇少年
 */
@Slf4j
public class MessagePipeScheduler {
    /**
     * The current scheduler bind {@link MessagePipe}
     */
    private MessagePipe messagePipe;
    /**
     * The distributor bound to {@link MessagePipe}
     */
    private MessagePipeDistributor distributor;

    public MessagePipeScheduler(MessagePipe messagePipe, MessagePipeDistributor messagePipeDistributor) {
        Assert.notNull(messagePipe, "The MessagePipe cannot be null.");
        Assert.notNull(messagePipeDistributor, "The MessagePipeDistributor cannot be null.");
        this.messagePipe = messagePipe;
        this.distributor = messagePipeDistributor;
    }

    /**
     * Start message distribution
     * <p>
     * There is a separate thread to run this method,
     * as long as there is a message that needs to be distributed in the message pipeline,
     * the thread will be awakened, otherwise it will be suspended
     */
    public void startup() {
        while (true) {
            try {
                log.debug("MessagePipe：{}，starting execution scheduler.", messagePipe.getName());
                messagePipe.handleFirst(message -> distributor.sendMessage(message));
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
    }
}
