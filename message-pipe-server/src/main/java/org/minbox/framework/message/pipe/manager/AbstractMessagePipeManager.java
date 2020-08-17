package org.minbox.framework.message.pipe.manager;

import lombok.extern.slf4j.Slf4j;
import org.minbox.framework.message.pipe.MessagePipe;
import org.minbox.framework.message.pipe.MessagePipeFactoryBean;
import org.minbox.framework.message.pipe.config.MessagePipeConfiguration;
import org.minbox.framework.message.pipe.exception.MessagePipeException;
import org.redisson.api.RedissonClient;

import java.util.Map;
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
    private MessagePipeConfiguration sharedConfiguration;
    /**
     * The {@link MessagePipe} factory bean
     */
    private MessagePipeFactoryBean messagePipeFactoryBean;

    private AbstractMessagePipeManager(MessagePipeFactoryBean messagePipeFactoryBean) {
        this.messagePipeFactoryBean = messagePipeFactoryBean;
        if (messagePipeFactoryBean == null) {
            throw new MessagePipeException("The MessagePipeFactoryBean is must not be null.");
        }
    }

    /**
     * Use the default {@link MessagePipeConfiguration} to initialize {@link MessagePipe} instance
     *
     * @param messagePipeFactoryBean Build {@link MessagePipe} factory bean
     * @param configuration          The default {@link MessagePipeConfiguration}，used by all {@link MessagePipe} create
     */
    public AbstractMessagePipeManager(MessagePipeFactoryBean messagePipeFactoryBean,
                                      MessagePipeConfiguration configuration) {
        this(messagePipeFactoryBean);
        this.sharedConfiguration = configuration;
    }

    /**
     * Initialize the {@link MessagePipeConfiguration} of different {@link MessagePipe}
     *
     * @param messagePipeFactoryBean Build {@link MessagePipe} factory bean
     * @param initConfigurations     Initialized {@link MessagePipeConfiguration} list
     */
    public AbstractMessagePipeManager(RedissonClient redissonClient, MessagePipeFactoryBean messagePipeFactoryBean,
                                      Map<String, MessagePipeConfiguration> initConfigurations) {
        this(messagePipeFactoryBean);
        this.useInitConfigurationsToCreateMessagePipe(initConfigurations);
    }

    @Override
    public MessagePipe getMessagePipe(String name) {
        synchronized (MESSAGE_PIPE_MAP) {
            MessagePipe messagePipe = MESSAGE_PIPE_MAP.get(name);
            if (messagePipe == null) {
                MessagePipeConfiguration configuration = this.getConfiguration();
                messagePipe = this.messagePipeFactoryBean.createMessagePipe(name, configuration);
                MESSAGE_PIPE_MAP.put(name, messagePipe);
                log.debug("MessagePipe：{}，write to cache collection after creation.", name);
            }
            return messagePipe;
        }
    }

    /**
     * Use init {@link MessagePipeConfiguration} to create {@link MessagePipe}
     *
     * @param initConfigurations The {@link MessagePipeConfiguration} init map
     */
    private void useInitConfigurationsToCreateMessagePipe(Map<String, MessagePipeConfiguration> initConfigurations) {
        if (initConfigurations == null || initConfigurations.size() == 0) {
            log.warn("The provided initial MessagePipeConfiguration list is empty, no creation is performed.");
            return;
        }
        initConfigurations.keySet().stream().forEach(name -> {
            MessagePipeConfiguration configuration = initConfigurations.get(name);
            MessagePipe messagePipe = this.messagePipeFactoryBean.createMessagePipe(name, configuration);
            MESSAGE_PIPE_MAP.put(name, messagePipe);
        });
    }

    /**
     * Get {@link MessagePipe} configuration instance
     * <p>
     * The first get it from the {@link #MESSAGE_PIPE_MAP}
     *
     * @return {@link MessagePipeConfiguration}
     */
    private MessagePipeConfiguration getConfiguration() {
        return sharedConfiguration == null ? MessagePipeConfiguration.defaultConfiguration() : sharedConfiguration;
    }
}
