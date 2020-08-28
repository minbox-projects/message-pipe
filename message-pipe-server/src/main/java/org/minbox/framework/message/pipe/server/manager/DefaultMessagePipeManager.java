package org.minbox.framework.message.pipe.server.manager;

import org.minbox.framework.message.pipe.server.MessagePipeFactoryBean;
import org.minbox.framework.message.pipe.server.config.MessagePipeConfiguration;
import org.minbox.framework.message.pipe.server.service.discovery.ServiceDiscovery;

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

    public DefaultMessagePipeManager(MessagePipeFactoryBean messagePipeFactoryBean,
                                     MessagePipeConfiguration configuration,
                                     ServiceDiscovery serviceDiscovery) {
        super(messagePipeFactoryBean, configuration, serviceDiscovery);
    }
}
