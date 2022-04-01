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
        Thread schedulerThread = new Thread(() -> {
            while (!messagePipe.isStopSchedulerThread()) {
                try {
                    messagePipe.handleFirst(message -> distributor.sendMessage(message));
                    log.debug("MessagePipe：{}，scheduler execution complete.", messagePipe.getName());
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            }
            log.warn("The MessagePipe：{}, scheduler thread stop successfully.", messagePipe.getName());
        });
        schedulerThread.setDaemon(true);
        schedulerThread.start();
    }
}
