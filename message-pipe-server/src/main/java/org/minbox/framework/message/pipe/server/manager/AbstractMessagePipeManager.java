package org.minbox.framework.message.pipe.server.manager;

import lombok.extern.slf4j.Slf4j;
import org.minbox.framework.message.pipe.core.exception.MessagePipeException;
import org.minbox.framework.message.pipe.server.MessagePipe;
import org.minbox.framework.message.pipe.server.MessagePipeFactoryBean;
import org.minbox.framework.message.pipe.server.config.MessagePipeConfiguration;
import org.minbox.framework.message.pipe.server.distribution.MessageDistributionExecutors;

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
    /**
     * The {@link MessageDistributionExecutors} instance
     */
    private MessageDistributionExecutors messageExecutors;

    private AbstractMessagePipeManager(MessagePipeFactoryBean messagePipeFactoryBean, MessageDistributionExecutors messageExecutors) {
        this.messagePipeFactoryBean = messagePipeFactoryBean;
        this.messageExecutors = messageExecutors;
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
                                      MessagePipeConfiguration configuration,
                                      MessageDistributionExecutors messageExecutors) {
        this(messagePipeFactoryBean, messageExecutors);
        this.sharedConfiguration = configuration;
    }

    /**
     * Initialize the {@link MessagePipeConfiguration} of different {@link MessagePipe}
     *
     * @param messagePipeFactoryBean Build {@link MessagePipe} factory bean
     * @param initConfigurations     Initialized {@link MessagePipeConfiguration} list
     */
    public AbstractMessagePipeManager(MessagePipeFactoryBean messagePipeFactoryBean,
                                      Map<String, MessagePipeConfiguration> initConfigurations,
                                      MessageDistributionExecutors messageExecutors) {
        this(messagePipeFactoryBean, messageExecutors);
        this.useInitConfigurationsToCreateMessagePipe(initConfigurations);
    }

    @Override
    public void createMessagePipe(String name) {
        synchronized (MESSAGE_PIPE_MAP) {
            if (!MESSAGE_PIPE_MAP.containsKey(name)) {
                MessagePipeConfiguration configuration = this.getConfiguration();
                MessagePipe messagePipe = this.messagePipeFactoryBean.createMessagePipe(name, configuration);
                MESSAGE_PIPE_MAP.put(name, messagePipe);
                // Start distribution executors
                messageExecutors.startExecutor(messagePipe);
                log.info("MessagePipe: {}, created successfully and cached.", name);
            }
        }
    }

    @Override
    public MessagePipe getMessagePipe(String name) {
        synchronized (MESSAGE_PIPE_MAP) {
            this.createMessagePipe(name);
            return MESSAGE_PIPE_MAP.get(name);
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
        initConfigurations.keySet().stream().forEach(name -> this.createMessagePipe(name));
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
