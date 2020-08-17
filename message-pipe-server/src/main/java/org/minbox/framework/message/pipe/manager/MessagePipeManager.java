package org.minbox.framework.message.pipe.manager;

import org.minbox.framework.message.pipe.MessagePipe;

/**
 * Message pipe manager
 *
 * @author 恒宇少年
 */
public interface MessagePipeManager {
    /**
     * Get message pipe by name
     *
     * @param name The {@link MessagePipe} name
     * @return {@link MessagePipe} instance
     */
    MessagePipe getMessagePipe(String name);
}
