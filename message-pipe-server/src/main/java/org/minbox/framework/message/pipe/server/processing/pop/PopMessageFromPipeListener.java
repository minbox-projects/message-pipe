package org.minbox.framework.message.pipe.server.processing.pop;

import lombok.extern.slf4j.Slf4j;
import org.minbox.framework.message.pipe.server.processing.EventPublisherKeyspaceMessageListener;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.util.ObjectUtils;

/**
 * Monitor the list in Redis to get data from the left
 *
 * @author 恒宇少年
 */
@Slf4j
public class PopMessageFromPipeListener extends EventPublisherKeyspaceMessageListener {
    /**
     * The bean name of {@link PopMessageFromPipeListener}
     */
    public static final String BEAN_NAME = "popMessageFromPipeListener";
    private static final String LEFT_POP_PATTERN_TOPIC = "__keyevent@*:lpop";

    public PopMessageFromPipeListener(RedisMessageListenerContainer listenerContainer) {
        super(listenerContainer);
    }

    @Override
    public PatternTopic patternTopicUsed() {
        return new PatternTopic(LEFT_POP_PATTERN_TOPIC);
    }

    @Override
    protected void doHandleMessage(Message message) {
        String redisQueueKey = message.toString();
        String pipeName = extractPipeName(redisQueueKey);
        if (ObjectUtils.isEmpty(pipeName)) {
            log.warn("The message pipe name was not extracted from Key: {}.", redisQueueKey);
            return;
        }
        // Publish PopMessageEvent
        PopMessageEvent popMessageEvent = new PopMessageEvent(this, pipeName);
        publishEvent(popMessageEvent);
        log.debug("Message Pipe：{}，publish PopMessageEvent successfully.", pipeName);
    }
}
