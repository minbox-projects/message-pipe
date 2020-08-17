package org.minbox.framework.message.pipe;

import org.minbox.framework.message.pipe.config.MessagePipeConfiguration;

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
    public MessagePipe createMessagePipe(MessagePipeConfiguration configuration) {
        return new MessagePipe(configuration);
    }
}
