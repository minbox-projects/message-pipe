package org.minbox.framework.message.pipe.manager;

import org.minbox.framework.message.pipe.MessagePipeFactoryBean;

/**
 * The {@link MessagePipeManager} default support
 *
 * @author 恒宇少年
 * @see AbstractMessagePipeManager
 */
public class DefaultMessagePipeManager extends AbstractMessagePipeManager {
    public DefaultMessagePipeManager(MessagePipeFactoryBean messagePipeFactoryBean) {
        super(messagePipeFactoryBean);
    }
}
