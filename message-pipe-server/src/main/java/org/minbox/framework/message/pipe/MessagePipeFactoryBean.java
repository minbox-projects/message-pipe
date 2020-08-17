package org.minbox.framework.message.pipe;

import org.minbox.framework.message.pipe.config.MessagePipeConfiguration;
import org.minbox.framework.message.pipe.exception.MessagePipeException;
import org.redisson.api.RedissonClient;

/**
 * The {@link MessagePipe} factory bean
 *
 * @author 恒宇少年
 */
public class MessagePipeFactoryBean {
    /**
     * The redisson client instance
     */
    private RedissonClient redissonClient;

    public MessagePipeFactoryBean(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
        if (this.redissonClient == null) {
            throw new MessagePipeException("The RedissonClient is must not be null.");
        }
    }

    /**
     * Create the new message pipe
     *
     * @param configuration The {@link MessagePipe} configuration
     * @return {@link MessagePipe} instance
     */
    public MessagePipe createMessagePipe(String name, MessagePipeConfiguration configuration) {
        return new MessagePipe(name, this.redissonClient, configuration);
    }
}
