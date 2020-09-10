package org.minbox.framework.message.pipe.server.processing.push;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.listener.KeyspaceEventMessageListener;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.util.ObjectUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.minbox.framework.message.pipe.core.PipeConstants.PIPE_NAME_PATTERN;

/**
 * Waiting for the message to be pushed to the listener of the pipeline
 * <p>
 * redis will push change messages according to the monitored key expression
 *
 * @author 恒宇少年
 */
@Slf4j
public class PushMessageToPipeListener extends KeyspaceEventMessageListener implements ApplicationEventPublisherAware {
    /**
     * The bean name of {@link PushMessageToPipeListener}
     */
    public static final String BEAN_NAME = "pushMessageListener";
    private static final String PUSH_PATTERN_TOPIC = "__keyevent@*__:rpush";
    private ApplicationEventPublisher applicationEventPublisher;

    public PushMessageToPipeListener(RedisMessageListenerContainer listenerContainer) {
        super(listenerContainer);
    }

    @Override
    protected void doRegister(RedisMessageListenerContainer container) {
        container.addMessageListener(this, new PatternTopic(PUSH_PATTERN_TOPIC));
    }

    @Override
    protected void doHandleMessage(Message message) {
        String redisQueueKey = message.toString();
        String pipeName = this.extractPipeName(redisQueueKey);
        if (ObjectUtils.isEmpty(pipeName)) {
            log.warn("The message pipe name was not extracted from Key: {}.", redisQueueKey);
            return;
        }
        // Publish PushMessageEvent
        PushMessageEvent pushMessageEvent = new PushMessageEvent(this, pipeName);
        applicationEventPublisher.publishEvent(pushMessageEvent);
        log.debug("Message Pipe：{}，publish PushMessageEvent successfully.", pipeName);
    }


    @Override
    public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    /**
     * Extract the pipeline name based on the Key in redis
     *
     * @param redisQueueKey The redis queue key
     *                      example："test.queue"
     * @return The name of message pipe,if the key does not match the expression, it returns null
     */
    private String extractPipeName(String redisQueueKey) {
        Pattern pipeKeyPattern = Pattern.compile(PIPE_NAME_PATTERN);
        Matcher matcher = pipeKeyPattern.matcher(redisQueueKey);
        return matcher.find() ? matcher.group(1) : null;
    }
}
