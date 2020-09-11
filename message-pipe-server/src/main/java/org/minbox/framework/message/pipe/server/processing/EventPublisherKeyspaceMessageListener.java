package org.minbox.framework.message.pipe.server.processing;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.data.redis.listener.KeyspaceEventMessageListener;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.minbox.framework.message.pipe.core.PipeConstants.PIPE_NAME_PATTERN;

/**
 * The {@link KeyspaceEventMessageListener} subclass
 * <p>
 * Encapsulate {@link KeyspaceEventMessageListener} ，
 * provide a method for publishing Spring {@link org.springframework.context.ApplicationEvent}
 *
 * @author 恒宇少年
 */
public abstract class EventPublisherKeyspaceMessageListener extends KeyspaceEventMessageListener implements ApplicationEventPublisherAware {
    private ApplicationEventPublisher applicationEventPublisher;

    public abstract PatternTopic patternTopicUsed();

    public EventPublisherKeyspaceMessageListener(RedisMessageListenerContainer listenerContainer) {
        super(listenerContainer);
    }

    @Override
    protected void doRegister(RedisMessageListenerContainer container) {
        container.addMessageListener(this, this.patternTopicUsed());
    }

    @Override
    public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    /**
     * Publish given {@link ApplicationEvent} instance
     *
     * @param event The {@link ApplicationEvent} instance
     */
    protected void publishEvent(ApplicationEvent event) {
        this.applicationEventPublisher.publishEvent(event);
    }

    /**
     * Extract the pipeline name based on the Key in redis
     *
     * @param redisQueueKey The redis queue key
     *                      example："test.queue"
     * @return The name of message pipe,if the key does not match the expression, it returns null
     */
    protected String extractPipeName(String redisQueueKey) {
        Pattern pipeKeyPattern = Pattern.compile(PIPE_NAME_PATTERN);
        Matcher matcher = pipeKeyPattern.matcher(redisQueueKey);
        return matcher.find() ? matcher.group(1) : null;
    }
}
