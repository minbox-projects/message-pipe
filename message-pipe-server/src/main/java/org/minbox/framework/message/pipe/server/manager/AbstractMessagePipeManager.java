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
import org.springframework.util.ObjectUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
    private static final int CLEANUP_EXPIRED_CORE_THREADS = 1;
    /**
     * Create the configuration object used by the {@link MessagePipe}
     */
    private MessagePipeConfiguration sharedConfiguration;
    private BeanFactory beanFactory;
    private static ScheduledExecutorService CLEANUP_EXPIRED_SERVICE;
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
    public MessagePipe createMessagePipe(String name) {
        synchronized (MESSAGE_PIPE_MAP) {
            if (!checkIsExclude(name) && !MESSAGE_PIPE_MAP.containsKey(name)) {
                log.info("Create new message pipe {},current number of cached is {}, max limit is {}.", name, MESSAGE_PIPE_MAP.size(),
                        serverConfiguration.getMaxMessagePipeCount());
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
                monitor.startup();
                log.info("MessagePipe：{}，monitor create successfully.", name);

                // Create MessagePipe Scheduler
                MessagePipeScheduler scheduler = new MessagePipeScheduler(messagePipe, distributor);
                scheduler.startup();
                log.info("MessagePipe：{}，scheduler create successfully.", name);
                return messagePipe;
            } else {
                return MESSAGE_PIPE_MAP.get(name);
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
        CLEANUP_EXPIRED_SERVICE = Executors.newScheduledThreadPool(CLEANUP_EXPIRED_CORE_THREADS);
        this.startCleanupExpiredThread();
        log.info("The MessagePipeManager startup successfully，maximum number of message pipes：{}.",
                serverConfiguration.getMaxMessagePipeCount());
    }

    /**
     * Start cleanup expired message pipe thread
     */
    private void startCleanupExpiredThread() {
        CLEANUP_EXPIRED_SERVICE.scheduleAtFixedRate(() -> {
            try {
                log.debug("Clean up expired message pipes thread is start working...");
                List<MessagePipe> expiredList = MESSAGE_PIPE_MAP.values().stream()
                        .filter(messagePipe -> {
                            long diffSeconds = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - messagePipe.getLastProcessTimeMillis());
                            return diffSeconds > serverConfiguration.getCleanupExpiredMessagePipeThresholdSeconds();
                        }).collect(Collectors.toList());
                if (!ObjectUtils.isEmpty(expiredList)) {
                    expiredList.stream().forEach(expiredMessagePipe -> {
                        try {
                            // stop scheduler thread
                            expiredMessagePipe.setStopSchedulerThread(true);
                            // stop monitor thread
                            expiredMessagePipe.setStopMonitorThread(true);
                            // remove from cache map
                            MESSAGE_PIPE_MAP.remove(expiredMessagePipe.getName(), expiredMessagePipe);
                            log.warn("The MessagePipe：{} is expired, threshold：{}, last process time is {}.", expiredMessagePipe.getName(),
                                    serverConfiguration.getCleanupExpiredMessagePipeThresholdSeconds(),
                                    new Date(expiredMessagePipe.getLastProcessTimeMillis()));
                        } catch (Exception e) {
                            log.error(e.getMessage(), e);
                        }
                    });
                    log.warn("The cleanup of expired message pipes thread is completed, this cleanup: {}.",
                            expiredList.stream().map(MessagePipe::getName).collect(Collectors.toList()));
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }, 1, serverConfiguration.getCleanupExpiredMessagePipeIntervalSeconds(), TimeUnit.SECONDS);
    }

    @Override
    public void destroy() throws Exception {
        redissonClient.shutdown();
        log.info("The MessagePipeManager shutdown successfully.");
    }

    /**
     * Check whether to exclude creating a message pipeline
     *
     * @param pipeName The name of message pipe
     * @return Return "true" if you need to exclude
     */
    private boolean checkIsExclude(String pipeName) {
        String[] excludes = serverConfiguration.getExcludePipeNamePatterns();
        if (ObjectUtils.isEmpty(excludes)) {
            return false;
        }
        boolean isExclude = false;
        for (String excludePattern : excludes) {
            Pattern pipeKeyPattern = Pattern.compile(excludePattern);
            Matcher matcher = pipeKeyPattern.matcher(pipeName);
            if (matcher.find()) {
                isExclude = true;
                log.warn("Message pipeline: {}, exclude creation.", pipeName);
                break;
            }
        }
        return isExclude;
    }
}
