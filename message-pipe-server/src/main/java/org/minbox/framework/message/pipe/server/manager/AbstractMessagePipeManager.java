package org.minbox.framework.message.pipe.server.manager;

import lombok.extern.slf4j.Slf4j;
import org.minbox.framework.message.pipe.core.exception.MessagePipeException;
import org.minbox.framework.message.pipe.server.MessagePipe;
import org.minbox.framework.message.pipe.server.config.MessagePipeConfiguration;
import org.minbox.framework.message.pipe.server.config.ServerConfiguration;
import org.minbox.framework.message.pipe.server.service.discovery.ServiceDiscovery;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The {@link MessagePipeManager} abstract implementation class
 *
 * @author 恒宇少年
 */
@Slf4j
public abstract class AbstractMessagePipeManager implements MessagePipeManager,
        InitializingBean, DisposableBean, BeanFactoryAware {
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
    private BeanFactory beanFactory;
    private static ExecutorService SCHEDULER_SERVICE;
    private static ExecutorService MONITOR_SERVICE;
    private ServerConfiguration serverConfiguration;
    private MessagePipeFactoryBean messagePipeFactoryBean;
    private ServiceDiscovery serviceDiscovery;
    private RedissonClient redissonClient;

    /**
     * Use the default {@link MessagePipeConfiguration} to initialize {@link MessagePipe} instance
     *
     * @param configuration The default {@link MessagePipeConfiguration}，used by all {@link MessagePipe} create
     */
    public AbstractMessagePipeManager(MessagePipeConfiguration configuration) {
        this.sharedConfiguration = configuration;
    }

    /**
     * Initialize the {@link MessagePipeConfiguration} of different {@link MessagePipe}
     *
     * @param initConfigurations Initialized {@link MessagePipeConfiguration} list
     */
    public AbstractMessagePipeManager(Map<String, MessagePipeConfiguration> initConfigurations) {
        this.useInitConfigurationsToCreateMessagePipe(initConfigurations);
    }

    @Override
    public void createMessagePipe(String name) {
        synchronized (MESSAGE_PIPE_MAP) {
            if (!MESSAGE_PIPE_MAP.containsKey(name)) {
                if (MESSAGE_PIPE_MAP.size() >= serverConfiguration.getMaxMessagePipeCount()) {
                    throw new MessagePipeException("The number of message pipes reaches the upper limit, " +
                            "and the message pipe cannot be created.");
                }
                MessagePipeConfiguration configuration = this.getConfiguration();
                MessagePipe messagePipe = this.messagePipeFactoryBean.createMessagePipe(name, configuration);
                MESSAGE_PIPE_MAP.put(name, messagePipe);
                log.info("MessagePipe：{}，created successfully and cached.", name);

                // Create MessagePipe Distributor
                MessagePipeDistributor distributor = new MessagePipeDistributor(messagePipe, serviceDiscovery);
                log.info("MessagePipe：{}，distributor create successfully.", name);

                // Create MessagePipe Monitor
                MessagePipeMonitor monitor = new MessagePipeMonitor(messagePipe, distributor);
                MONITOR_SERVICE.submit(() -> monitor.startup());
                log.info("MessagePipe：{}，monitor create successfully.", name);

                // Create MessagePipe Scheduler
                MessagePipeScheduler scheduler = new MessagePipeScheduler(messagePipe, distributor);
                SCHEDULER_SERVICE.submit(() -> scheduler.startup());
                log.info("MessagePipe：{}，scheduler create successfully.", name);
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

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        this.redissonClient = beanFactory.getBean(RedissonClient.class);
        this.serverConfiguration = beanFactory.getBean(ServerConfiguration.class);
        this.messagePipeFactoryBean = beanFactory.getBean(MessagePipeFactoryBean.class);
        this.serviceDiscovery = beanFactory.getBean(ServiceDiscovery.class);
        SCHEDULER_SERVICE = Executors.newFixedThreadPool(serverConfiguration.getMaxMessagePipeCount());
        MONITOR_SERVICE = Executors.newFixedThreadPool(serverConfiguration.getMaxMessagePipeCount());
        log.info("The MessagePipeManager startup successfully，maximum number of message pipes：{}.",
                serverConfiguration.getMaxMessagePipeCount());
    }

    @Override
    public void destroy() throws Exception {
        SCHEDULER_SERVICE.shutdown();
        MONITOR_SERVICE.shutdown();
        redissonClient.shutdown();
        log.info("The MessagePipeManager shutdown successfully.");
    }
}
