package org.minbox.framework.message.pipe.server.manager;

import org.minbox.framework.message.pipe.server.config.MessagePipeConfiguration;

/**
 * The {@link MessagePipeManager} default support
 *
 * @author 恒宇少年
 * @see AbstractMessagePipeManager
 */
public class DefaultMessagePipeManager extends AbstractMessagePipeManager {
    /**
     * The bean name of {@link DefaultMessagePipeManager}
     */
    public static final String BEAN_NAME = "defaultMessagePipeManager";

    /**
     * Constructs a new DefaultMessagePipeManager instance
     *
     * @param configuration the message pipe configuration
     */
    public DefaultMessagePipeManager(MessagePipeConfiguration configuration) {
        super(configuration);
    }
}
