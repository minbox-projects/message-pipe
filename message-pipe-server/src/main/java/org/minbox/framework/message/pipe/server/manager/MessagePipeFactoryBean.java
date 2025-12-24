package org.minbox.framework.message.pipe.server.manager;

import org.minbox.framework.message.pipe.core.exception.MessagePipeException;
import org.minbox.framework.message.pipe.server.MessagePipe;
import org.minbox.framework.message.pipe.server.config.MessagePipeConfiguration;
import org.minbox.framework.message.pipe.server.service.discovery.ServiceDiscovery;
import org.redisson.api.RedissonClient;

/**
 * The {@link MessagePipe} factory bean
 *
 * @author 恒宇少年
 */
public class MessagePipeFactoryBean {
    /**
     * The bean name of {@link MessagePipeFactoryBean}
     */
    public static final String BEAN_NAME = "messagePipeFactoryBean";
    /**
     * The redisson client instance
     */
    private RedissonClient redissonClient;
    /**
     * The service discovery instance
     */
    private ServiceDiscovery serviceDiscovery;

    public MessagePipeFactoryBean(RedissonClient redissonClient, ServiceDiscovery serviceDiscovery) {
        this.redissonClient = redissonClient;
        this.serviceDiscovery = serviceDiscovery;
        if (this.redissonClient == null) {
            throw new MessagePipeException("The RedissonClient is must not be null.");
        }
        if (this.serviceDiscovery == null) {
            throw new MessagePipeException("The ServiceDiscovery is must not be null.");
        }
    }

    /**
     * Create the new message pipe
     *
     * @param name          The {@link MessagePipe} name
     * @param configuration The {@link MessagePipe} configuration
     * @return {@link MessagePipe} instance
     */
    public MessagePipe createMessagePipe(String name, MessagePipeConfiguration configuration) {
        return new MessagePipe(name, this.redissonClient, configuration, this.serviceDiscovery);
    }
}
