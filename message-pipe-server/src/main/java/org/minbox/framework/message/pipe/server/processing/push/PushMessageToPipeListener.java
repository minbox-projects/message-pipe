package org.minbox.framework.message.pipe.server.processing.push;

import lombok.extern.slf4j.Slf4j;
import org.minbox.framework.message.pipe.server.processing.EventPublisherKeyspaceMessageListener;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.util.ObjectUtils;

/**
 * Waiting for the message to be pushed to the listener of the pipeline
 * <p>
 * redis will push change messages according to the monitored key expression
 *
 * @author 恒宇少年
 */
@Slf4j
public class PushMessageToPipeListener extends EventPublisherKeyspaceMessageListener {
    /**
     * The bean name of {@link PushMessageToPipeListener}
     */
    public static final String BEAN_NAME = "pushMessageListener";
    private static final String PUSH_PATTERN_TOPIC = "__keyevent@*__:rpush";

    public PushMessageToPipeListener(RedisMessageListenerContainer listenerContainer) {
        super(listenerContainer);
    }

    @Override
    public PatternTopic patternTopicUsed() {
        return new PatternTopic(PUSH_PATTERN_TOPIC);
    }

    @Override
    protected void doHandleMessage(Message message) {
        String redisQueueKey = message.toString();
        String pipeName = extractPipeName(redisQueueKey);
        if (ObjectUtils.isEmpty(pipeName)) {
            log.warn("The message pipe name was not extracted from Key: {}.", redisQueueKey);
            return;
        }
        // Publish PushMessageEvent
        PushMessageEvent pushMessageEvent = new PushMessageEvent(this, pipeName);
        publishEvent(pushMessageEvent);
        log.debug("Message Pipe：{}，publish PushMessageEvent successfully.", pipeName);
    }
}
