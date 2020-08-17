package org.minbox.framework.message.pipe;

import org.minbox.framework.message.pipe.config.MessagePipeConfiguration;
import org.redisson.api.RedissonClient;

/**
 * The {@link MessagePipe} factory bean
 *
 * @author 恒宇少年
 */
public class MessagePipeFactoryBean {
    /**
     * Create the new message pipe
     *
     * @param configuration The {@link MessagePipe} configuration
     * @return {@link MessagePipe} instance
     */
    public MessagePipe createMessagePipe(String name, RedissonClient redissonClient, MessagePipeConfiguration configuration) {
        return new MessagePipe(name, redissonClient, configuration);
    }
}
