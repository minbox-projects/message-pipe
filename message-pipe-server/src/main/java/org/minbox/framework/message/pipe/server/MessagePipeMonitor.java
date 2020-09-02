package org.minbox.framework.message.pipe.server;

import lombok.extern.slf4j.Slf4j;
import org.minbox.framework.message.pipe.server.config.ServerConfiguration;
import org.minbox.framework.message.pipe.server.distribution.MessageDistributionExecutor;
import org.minbox.framework.message.pipe.server.distribution.MessageDistributionExecutors;
import org.minbox.framework.message.pipe.server.manager.MessagePipeManager;
import org.springframework.beans.factory.InitializingBean;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Message pipeline monitoring object
 * <p>
 * Monitor the number of messages remaining in each message pipeline
 * the time interval since the last execution of the message distribution
 *
 * @author 恒宇少年
 */
@Slf4j
public class MessagePipeMonitor implements InitializingBean {
    /**
     * The bean name of {@link MessagePipeMonitor}
     */
    public static final String BEAN_NAME = "messagePipeMonitor";
    private static ScheduledExecutorService monitorThreadPool = Executors.newScheduledThreadPool(1);
    private ServerConfiguration configuration;
    private MessagePipeManager messagePipeManager;
    private MessageDistributionExecutors messageDistributionExecutors;

    public MessagePipeMonitor(ServerConfiguration serverConfiguration,
                              MessagePipeManager messagePipeManager,
                              MessageDistributionExecutors messageDistributionExecutors) {
        this.configuration = serverConfiguration;
        this.messagePipeManager = messagePipeManager;
        this.messageDistributionExecutors = messageDistributionExecutors;
    }

    /**
     * Starting message pipe executor monitor
     * <p>
     * When judging that the number of messages in the message pipeline is greater than 0,
     * and the time interval from the last execution exceeds the configured threshold
     * notify {@link MessageDistributionExecutor} execution message distribution
     */
    private void startMonitor() {
        try {
            List<MessageDistributionExecutor> executors = messageDistributionExecutors.getExecutors();
            executors.stream().forEach(executor -> {
                String pipeName = executor.getPipeName();
                Long currentTimeMillis = System.currentTimeMillis();
                MessagePipe messagePipe = messagePipeManager.getMessagePipe(pipeName);
                long intervals = currentTimeMillis - messagePipe.getLastProcessTimeMillis();
                log.debug("MessagePipe：{}，Interval execution mill seconds：{}", pipeName, intervals);
                if (messagePipe.size() > 0 && intervals > configuration.getNotifyIntervalMillSeconds()) {
                    messageDistributionExecutors.notifyExecutor(pipeName);
                    log.debug("MessagePipe：{}，MessageDistributionExecutor be awakened.", pipeName);
                }
            });
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        monitorThreadPool.scheduleWithFixedDelay(() -> this.startMonitor(),
                10, configuration.getMonitorCheckIntervalSeconds(), TimeUnit.SECONDS);
        log.info("MessagePipe monitor start successfully.");
    }
}
