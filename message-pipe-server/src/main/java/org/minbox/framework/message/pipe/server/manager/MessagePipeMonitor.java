package org.minbox.framework.message.pipe.server.manager;

import lombok.extern.slf4j.Slf4j;
import org.minbox.framework.message.pipe.core.exception.MessagePipeException;
import org.minbox.framework.message.pipe.server.MessagePipe;
import org.minbox.framework.message.pipe.server.config.MessagePipeConfiguration;
import org.springframework.util.Assert;

/**
 * Message pipeline monitoring object
 * <p>
 * Monitor the number of messages remaining in each message pipeline
 * the time interval since the last execution of the message distribution
 *
 * @author 恒宇少年
 */
@Slf4j
public class MessagePipeMonitor {
    /**
     * The monitor bound to {@link MessagePipe}
     */
    private MessagePipe messagePipe;
    /**
     * The distributor bound to {@link MessagePipe}
     */
    private MessagePipeDistributor distributor;
    /**
     * Configuration object for each message pipeline
     */
    private MessagePipeConfiguration configuration;

    public MessagePipeMonitor(MessagePipe messagePipe, MessagePipeDistributor messagePipeDistributor) {
        Assert.notNull(messagePipe, "The MessagePipe cannot be null.");
        Assert.notNull(messagePipeDistributor, "The MessagePipeDistributor cannot be null.");
        this.messagePipe = messagePipe;
        this.distributor = messagePipeDistributor;
        this.configuration = messagePipe.getConfiguration();
    }

    /**
     * Starting message pipe executor monitor
     * <p>
     * Perform message monitoring in the message pipeline at intervals.
     * If there is a message and the time from the last single execution exceeds the threshold,
     * perform all message distribution
     */
    public void startup() {
        while (true) {
            try {
                messagePipe.handleToLast(message -> distributor.sendMessage(message));
                log.debug("MessagePipe：{}，execution monitor complete.", messagePipe.getName());
                Thread.sleep(configuration.getMessagePipeMonitorMillis());
            }catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error(e.getMessage(), e);
            }
        }
    }
}
