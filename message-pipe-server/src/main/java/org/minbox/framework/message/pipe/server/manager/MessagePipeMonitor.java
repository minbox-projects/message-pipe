package org.minbox.framework.message.pipe.server.manager;

import lombok.extern.slf4j.Slf4j;
import org.minbox.framework.message.pipe.server.MessagePipe;
import org.minbox.framework.message.pipe.server.config.MessagePipeConfiguration;
import org.springframework.util.Assert;

/**
 * Message pipeline monitoring object
 * <p>
 * Monitor the number of messages remaining in each message pipeline
 * the time interval since the last execution of the message distribution
 *
 * @author 恒宇少年
 */
@Slf4j
public class MessagePipeMonitor {
    /**
     * The monitor bound to {@link MessagePipe}
     */
    private final MessagePipe messagePipe;
    /**
     * The distributor bound to {@link MessagePipe}
     */
    private final MessagePipeDistributor distributor;
    /**
     * Configuration object for each message pipeline
     */
    private final MessagePipeConfiguration configuration;

    public MessagePipeMonitor(MessagePipe messagePipe, MessagePipeDistributor messagePipeDistributor) {
        Assert.notNull(messagePipe, "The MessagePipe cannot be null.");
        Assert.notNull(messagePipeDistributor, "The MessagePipeDistributor cannot be null.");
        this.messagePipe = messagePipe;
        this.distributor = messagePipeDistributor;
        this.configuration = messagePipe.getConfiguration();

        MessagePipeMetricsAggregator.getInstance().register(
            messagePipe.getName(),
            this
        );
    }

    /**
     * Starting message pipe executor monitor
     * <p>
     * Perform message monitoring in the message pipeline at intervals.
     * If there is a message and the time from the last single execution exceeds the threshold,
     * perform all message distribution
     */
    public void startup() {
        MessagePipeMetricsAggregator.getInstance().startAggregationReporting();

        Thread monitorThread = new Thread(() -> {
            while (true) {
                try {
                    // Core business logic: process messages
                    messagePipe.handleToLast(distributor::sendMessage);

                    if (messagePipe.isStopMonitorThread()) {
                        break;
                    }

                    log.debug("MessagePipe：{}，execution monitor complete.",
                            messagePipe.getName());

                    Thread.sleep(configuration.getMessagePipeMonitorMillis());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.error(e.getMessage(), e);
                }
            }

            log.warn("The MessagePipe：{}, monitor thread stop successfully.", messagePipe.getName());
        });
        monitorThread.setName("MessagePipeMonitor-" + messagePipe.getName());
        monitorThread.setDaemon(true);
        monitorThread.start();
    }

    /**
     * Record dropped messages
     * <p>
     * Directly notifies the global aggregator to track dropped messages.
     */
    public void recordDroppedMessage() {
        MessagePipeMetricsAggregator.getInstance().recordDroppedMessage(messagePipe.getName());
    }

    /**
     * Get current metrics
     * <p>
     * Returns the current queue state. Historical metrics are maintained by MessagePipeMetricsAggregator.
     *
     * @return MonitoringMetrics object with current pipeline state
     */
    public MonitoringMetrics getMetrics() {
        return new MonitoringMetrics(
                messagePipe.getName(),
                messagePipe.size()
        );
    }

    /**
     * Monitoring metrics data class
     */
    public static class MonitoringMetrics {
        public final String pipeName;
        public final int currentQueueSize;

        public MonitoringMetrics(String pipeName, int currentQueueSize) {
            this.pipeName = pipeName;
            this.currentQueueSize = currentQueueSize;
        }
    }
}
