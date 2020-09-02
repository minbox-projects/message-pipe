package org.minbox.framework.message.pipe.server.distribution;

import lombok.extern.slf4j.Slf4j;
import org.minbox.framework.message.pipe.core.exception.MessagePipeException;
import org.minbox.framework.message.pipe.server.MessagePipe;
import org.minbox.framework.message.pipe.server.config.ServerConfiguration;
import org.minbox.framework.message.pipe.server.service.discovery.ServiceDiscovery;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.ObjectUtils;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * The {@link MessageDistributionExecutor} Collection processing class
 * <p>
 * After creating {@link MessageDistributionExecutor}
 * cache it and provide it to the {@link MessageDistributionExecutor#notify()} method
 *
 * @author 恒宇少年
 */
@Slf4j
public class MessageDistributionExecutors implements InitializingBean {
    /**
     * The name of {@link MessageDistributionExecutors}
     */
    public static final String BEAN_NAME = "messageDistributionExecutors";
    private static ExecutorService executorThreadPool;
    private static final ConcurrentMap<String, MessageDistributionExecutor> EXECUTORS = new ConcurrentHashMap<>();
    private ServerConfiguration serverConfiguration;
    private ServiceDiscovery serviceDiscovery;

    public MessageDistributionExecutors(ServerConfiguration serverConfiguration,
                                        ServiceDiscovery serviceDiscovery) {
        this.serverConfiguration = serverConfiguration;
        this.serviceDiscovery = serviceDiscovery;
    }

    /**
     * Create {@link MessageDistributionExecutor} instance
     * <p>
     * Assign a child thread to the MessageDistributionExecutor of each pipe
     * through the thread pool to prevent the main thread from blocking
     *
     * @param messagePipe The message pipe instance
     */
    public void startExecutor(MessagePipe messagePipe) {
        String pipeName = messagePipe.getName();
        if (!EXECUTORS.containsKey(pipeName)) {
            MessageDistributionExecutor executor =
                    new MessageDistributionExecutor(messagePipe, this.serviceDiscovery);
            EXECUTORS.put(pipeName, executor);
            executorThreadPool.submit(() -> executor.waitProcessing());
            log.info("MessagePipe：{}，distribution executors start successfully.", pipeName);
        }
    }

    /**
     * Wake up {@link MessageDistributionExecutor} of the specified pipe
     *
     * @param pipeName The name of message pipe
     */
    public void notifyExecutor(String pipeName) {
        MessageDistributionExecutor executor = EXECUTORS.get(pipeName);
        if (ObjectUtils.isEmpty(executor)) {
            throw new MessagePipeException("Message pipeline: " + pipeName + ", MessageDistributionExecutor does not exist.");
        }
        synchronized (executor) {
            executor.notifyAll();
            log.debug("Message Pipe：{}，MessageDistributionExecutor notify successfully.", pipeName);
        }
    }

    /**
     * Get all {@link MessageDistributionExecutor}
     *
     * @return The {@link MessageDistributionExecutor} for all message pipe
     */
    public List<MessageDistributionExecutor> getExecutors() {
        return EXECUTORS.values().stream().collect(Collectors.toList());
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        executorThreadPool = Executors.newFixedThreadPool(this.serverConfiguration.getMessageDistributionExecutorPoolSize());
    }
}
