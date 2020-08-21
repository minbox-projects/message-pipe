package org.minbox.framework.message.pipe.server.manager;

import org.minbox.framework.message.pipe.server.MessagePipe;

/**
 * Message pipe manager
 *
 * @author 恒宇少年
 */
public interface MessagePipeManager {
    /**
     * Create message pipe by name
     *
     * @param name The {@link MessagePipe} name
     * @return {@link MessagePipe} instance
     */
    void createMessagePipe(String name);

    /**
     * Get message pipe by name
     *
     * @param name The {@link MessagePipe} name
     * @return {@link MessagePipe} instance
     */
    MessagePipe getMessagePipe(String name);
}
