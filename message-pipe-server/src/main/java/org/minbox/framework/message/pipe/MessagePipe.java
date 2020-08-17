package org.minbox.framework.message.pipe;

import org.minbox.framework.message.pipe.config.MessagePipeConfiguration;
import org.minbox.framework.message.pipe.core.Message;
import org.redisson.api.RedissonClient;

/**
 * The message pipe
 *
 * @author 恒宇少年
 */
public class MessagePipe {
    // TODO 实例化RedissonClient对象
    private RedissonClient redissonClient;
    private MessagePipeConfiguration configuration;

    public MessagePipe(MessagePipeConfiguration configuration) {
        this.configuration = configuration;
    }

    /**
     * put message to pipe
     *
     * @param message The {@link Message} instance
     */
    public void put(Message message) {
        // TODO 写入管道
    }

    /**
     * take a message from pipe
     *
     * @return The {@link Message} instance
     */
    public Message take() {
        return null;
    }

}
