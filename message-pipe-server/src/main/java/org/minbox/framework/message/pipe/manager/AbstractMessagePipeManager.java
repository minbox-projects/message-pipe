package org.minbox.framework.message.pipe.manager;

import lombok.extern.slf4j.Slf4j;
import org.minbox.framework.message.pipe.MessagePipe;
import org.minbox.framework.message.pipe.MessagePipeFactoryBean;
import org.minbox.framework.message.pipe.config.MessagePipeConfiguration;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * The {@link MessagePipeManager} abstract implementation class
 *
 * @author 恒宇少年
 */
@Slf4j
public abstract class AbstractMessagePipeManager implements MessagePipeManager {
    /**
     * Store all {@link MessagePipe} object instances
     * <p>
     * The Key of the Map is the name of the {@link MessagePipe}
     */
    private static final ConcurrentMap<String, MessagePipe> MESSAGE_PIPE_MAP = new ConcurrentHashMap();
    /**
     * Create the configuration object used by the {@link MessagePipe}
     */
    private MessagePipeConfiguration configuration;
    /**
     * The {@link MessagePipe} factory bean
     */
    private MessagePipeFactoryBean messagePipeFactoryBean;

    public AbstractMessagePipeManager(MessagePipeConfiguration configuration, MessagePipeFactoryBean messagePipeFactoryBean) {
        this.configuration = configuration;
        this.messagePipeFactoryBean = messagePipeFactoryBean;
    }

    @Override
    public MessagePipe getMessagePipe(String name) {
        MessagePipe messagePipe = MESSAGE_PIPE_MAP.get(name);
        if (messagePipe == null) {
            messagePipe = this.messagePipeFactoryBean.createMessagePipe(configuration);
            MESSAGE_PIPE_MAP.put(name, messagePipe);
            log.debug("MessagePipe：{}，write to cache collection after creation.", name);
        }
        return messagePipe;
    }
}
