package org.minbox.framework.message.pipe.server.processing;

import lombok.extern.slf4j.Slf4j;
import org.minbox.framework.message.pipe.server.distribution.MessageDistributionExecutor;
import org.minbox.framework.message.pipe.server.distribution.MessageDistributionExecutors;
import org.springframework.context.ApplicationListener;
import org.springframework.util.Assert;

/**
 * Message processing listener
 * <p>
 * Handle different logic according to {@link MessageProcessingType}
 *
 * @author 恒宇少年
 */
@Slf4j
public class MessageProcessingListener implements ApplicationListener<MessageProcessingEvent> {
    /**
     * The bean name of {@link MessageProcessingListener}
     */
    public static final String BEAN_NAME = "messageProcessingListener";
    private MessageDistributionExecutors messageExecutors;

    public MessageProcessingListener(MessageDistributionExecutors messageExecutors) {
        this.messageExecutors = messageExecutors;
    }

    @Override
    public void onApplicationEvent(MessageProcessingEvent event) {
        String pipeName = event.getPipeName();
        MessageProcessingType processingType = event.getProcessingType();
        Assert.notNull(pipeName, "The message pipe name cannot be null.");
        Assert.notNull(processingType, "The message processing type cannot be null.");
        log.debug("Processing pipe：{}，type：{}.", pipeName, processingType);
        switch (processingType) {
            case PUSH:
                this.processingPushMessage(pipeName);
                break;
        }
    }

    /**
     * Processing {@link MessageProcessingType#PUSH} message
     * <p>
     * use {@link MessageDistributionExecutors#notifyExecutor} method，
     * wake up {@link MessageDistributionExecutor} execution distribution message to client
     *
     * @param pipeName The name of message pipe
     */
    private void processingPushMessage(String pipeName) {
        messageExecutors.notifyExecutor(pipeName);
    }
}
