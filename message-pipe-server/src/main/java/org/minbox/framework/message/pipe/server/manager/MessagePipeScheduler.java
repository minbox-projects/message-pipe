package org.minbox.framework.message.pipe.server.manager;

import lombok.extern.slf4j.Slf4j;
import org.minbox.framework.message.pipe.server.MessagePipe;
import org.springframework.util.Assert;

/**
 * Message scheduling class in message pipeline
 *
 * @author 恒宇少年
 */
@Slf4j
public class MessagePipeScheduler {
    /**
     * The current scheduler bind {@link MessagePipe}
     */
    private final MessagePipe messagePipe;
    /**
     * The distributor bound to {@link MessagePipe}
     */
    private final MessagePipeDistributor distributor;

    public MessagePipeScheduler(MessagePipe messagePipe, MessagePipeDistributor messagePipeDistributor) {
        Assert.notNull(messagePipe, "The MessagePipe cannot be null.");
        Assert.notNull(messagePipeDistributor, "The MessagePipeDistributor cannot be null.");
        this.messagePipe = messagePipe;
        this.distributor = messagePipeDistributor;
    }

    /**
     * Start message distribution
     * <p>
     * Merged Scheduler and Monitor into a single worker thread.
     */
    public void startup() {
        Thread schedulerThread = new Thread(() -> {
            while (!messagePipe.isStopSchedulerThread()) {
                try {
                    // 1. Check for healthy clients before attempting to process
                    if (!distributor.hasHealthyClient()) {
                        // If no healthy clients, just wait for the monitor interval (heartbeat check)
                        synchronized (messagePipe) {
                             if (!messagePipe.isStopSchedulerThread()) {
                                 messagePipe.wait(messagePipe.getConfiguration().getMessagePipeMonitorMillis());
                             }
                        }
                        continue;
                    }

                    // 2. Wait for new messages or timeout (Monitor logic)
                    synchronized (messagePipe) {
                        if (messagePipe.size() <= 0 && !messagePipe.isStopSchedulerThread()) {
                            messagePipe.wait(messagePipe.getConfiguration().getMessagePipeMonitorMillis());
                        }
                    }

                    // 3. Process all available messages (Batch Mode)
                    // handleToLast will loop until queue is empty or error occurs
                    if (messagePipe.size() > 0) {
                        messagePipe.handleToLast(distributor::sendMessageBatch, distributor::resolveClient);
                    }
                    
                    log.debug("MessagePipe：{}，scheduler execution complete.", messagePipe.getName());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    log.error("Error in MessagePipe worker: " + messagePipe.getName(), e);
                }
            }
            log.warn("The MessagePipe：{}, scheduler thread stop successfully.", messagePipe.getName());
        });
        schedulerThread.setName("PipeWorker-" + messagePipe.getName());
        schedulerThread.setDaemon(true);
        schedulerThread.start();
    }
}
