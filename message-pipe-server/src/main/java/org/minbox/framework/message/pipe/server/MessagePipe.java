package org.minbox.framework.message.pipe.server;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.minbox.framework.message.pipe.core.Message;
import org.minbox.framework.message.pipe.core.exception.MessagePipeException;
import org.minbox.framework.message.pipe.core.transport.MessageResponseStatus;
import org.minbox.framework.message.pipe.server.config.LockNames;
import org.minbox.framework.message.pipe.server.config.MessagePipeConfiguration;
import org.minbox.framework.message.pipe.server.exception.ExceptionHandler;
import org.minbox.framework.message.pipe.server.manager.MessageProcessStatus;
import org.minbox.framework.message.pipe.server.manager.MessageRetryRecord;
import org.minbox.framework.message.pipe.server.manager.MessageDeadLetterQueue;
import org.minbox.framework.message.pipe.server.manager.MessageRetryScheduler;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RLock;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

/**
 * The message pipe
 *
 * @author 恒宇少年
 */
@Slf4j
public class MessagePipe {
    /**
     * Name of current message pipe
     * <p>
     * this name is used to create the {@link RBlockingQueue} and {@link RLock}
     * the format is:#name.queues"、"#name.write.lock"、"#name.read.lock"
     */
    @Getter
    private String name;
    /**
     * The Redis blocking queue bound to the current message pipeline
     */
    @Getter
    private RBlockingQueue<Message> queue;
    /**
     * The redisson client instance
     *
     * @see RBlockingQueue
     * @see RLock
     */
    @Getter
    private RedissonClient redissonClient;
    /**
     * The queue name in redis
     */
    private String queueName;
    /**
     * The retry records map name in redis
     * <p>
     * Format: {pipeName}_retry_records
     */
    private String retryRecordsMapName;
    /**
     * The name of the lock used when putting the message
     */
    private final String putLockName;
    /**
     * The lock name used when taking the message
     */
    private final String takeLockName;
    /**
     * The last processing message millis
     * <p>
     * The default values is {@link System#currentTimeMillis()}
     */
    private final AtomicLong lastProcessTimeMillis = new AtomicLong(System.currentTimeMillis());
    /**
     * Whether the message monitoring method is being executed
     */
    private volatile boolean runningHandleAll = false;
    /**
     * Is the add data method being executed
     */
    private volatile boolean transfer = false;
    /**
     * The {@link MessagePipe} configuration
     */
    @Getter
    private MessagePipeConfiguration configuration;
    /**
     * Monitor the thread that processes the latest data from the message pipeline
     */
    @Getter
    @Setter
    private boolean isStopMonitorThread;
    /**
     * Schedule threads that process all data in the message pipeline regularly
     */
    @Getter
    @Setter
    private boolean isStopSchedulerThread;
    /**
     * Dead letter queue manager
     */
    @Getter
    private MessageDeadLetterQueue messageDeadLetterQueue;
    /**
     * Message retry scheduler
     */
    @Getter
    private MessageRetryScheduler messageRetryScheduler;


    /**
     * Retry queue name format: {pipeName}_retry
     */
    private static final String RETRY_RECORDS_QUEUE_NAME_FORMAT = "%s_retry_records";


    public MessagePipe(String name,
                       RedissonClient redissonClient,
                       MessagePipeConfiguration configuration) {
        this.name = name;
        this.queueName = LockNames.MESSAGE_QUEUE.format(this.name);
        this.retryRecordsMapName = String.format(RETRY_RECORDS_QUEUE_NAME_FORMAT, this.name);
        this.putLockName = LockNames.PUT_MESSAGE.format(this.name);
        this.takeLockName = LockNames.TAKE_MESSAGE.format(this.name);
        this.redissonClient = redissonClient;
        this.configuration = configuration;
        this.queue = redissonClient.getBlockingQueue(this.queueName, configuration.getCodec());

        // Initialize DLQ and retry scheduler
        this.messageDeadLetterQueue = new MessageDeadLetterQueue(redissonClient, name, configuration);
        this.messageRetryScheduler = new MessageRetryScheduler(redissonClient, name);

        if (this.name == null || this.name.trim().length() == 0) {
            throw new MessagePipeException("The MessagePipe name is required，cannot be null.");
        }
        if (this.redissonClient == null) {
            throw new MessagePipeException("The RedissonClient cannot be null.");
        }
        if (this.configuration == null) {
            throw new MessagePipeException("The MessagePipeConfiguration cannot be null.");
        }
    }

    /**
     * put message to current {@link MessagePipe} with {@link RLock}
     *
     * @param message The {@link Message} instance
     */
    public void putLastOnLock(Message message) {
        this.transfer = true;
        RLock putLock = redissonClient.getLock(putLockName);
        try {
            MessagePipeConfiguration.LockTime lockTime = configuration.getLockTime();
            if (putLock.tryLock(lockTime.getWaitTime(), lockTime.getLeaseTime(), lockTime.getTimeUnit())) {
                boolean addSuccess = queue.offer(message);
                if (!addSuccess) {
                    throw new MessagePipeException("Unsuccessful when writing the message to the queue.");
                }
            }
        } catch (Exception e) {
            this.doHandleException(e, MessageProcessStatus.PUT_EXCEPTION, message);
        } finally {
            this.transfer = false;
            if (putLock.isLocked() && putLock.isHeldByCurrentThread()) {
                putLock.unlock();
            }
            synchronized (this) {
                notifyAll();
            }
        }
    }

    /**
     * put message to current {@link MessagePipe}
     *
     * @param message The {@link Message} instance
     */
    public void putLast(Message message) {
        log.debug("write the last new message, content：{}.", message);
        this.transfer = true;
        try {
            boolean addSuccess = queue.offer(message);
            if (!addSuccess) {
                throw new MessagePipeException("Unsuccessful when writing the message to the queue.");
            }
        } catch (Exception e) {
            this.doHandleException(e, MessageProcessStatus.PUT_EXCEPTION, message);
        } finally {
            this.transfer = false;
            synchronized (this) {
                notifyAll();
            }
        }
    }

    /**
     * Processing first message
     *
     * @param function Logical method of processing first message in {@link MessagePipe}
     */
    public void handleFirst(Function<Message, MessageProcessStatus> function) {
        synchronized (this) {
            while (!isStopSchedulerThread && (transfer || runningHandleAll || queue.isEmpty())) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.error(e.getMessage(), e);
                }
            }
        }
        if (isStopSchedulerThread) {
            return;
        }
        log.debug("The message pipe：{} scheduler thread is woken up, handing first message.", name);
        Message current = null;
        MessageProcessStatus status = MessageProcessStatus.SEND_SUCCESS;
        RLock takeLock = redissonClient.getLock(takeLockName);
        try {
            MessagePipeConfiguration.LockTime lockTime = configuration.getLockTime();
            if (takeLock.tryLock(lockTime.getWaitTime(), lockTime.getLeaseTime(), lockTime.getTimeUnit())) {
                // Take first message
                current = this.peek();
                if (ObjectUtils.isEmpty(current)) {
                    log.error("Message pipeline: {}, no message to be processed was found.", name);
                    return;
                }
                status = function.apply(current);

                // Enhanced decision logic based on status
                switch (status) {
                    case SEND_SUCCESS:
                        // Message sent successfully - remove from queue
                        this.poll();
                        recordSuccess(current);
                        break;

                    case SEND_EXCEPTION:
                        // Message failed - check if should retry
                        handleMessageFailure(current);
                        break;

                    case NO_HEALTH_CLIENT:
                        // No healthy client - don't count as failure
                        log.error("No healthy client available, will retry later: {}",
                                new String(current.getBody()));
                        // DO NOT poll() - keep message in queue
                        break;
                }

                // Set last process time
                lastProcessTimeMillis.set(System.currentTimeMillis());
            }
        } catch (Exception e) {
            this.doHandleException(e, status, current);
        } finally {
            if (takeLock.isLocked() && takeLock.isHeldByCurrentThread()) {
                takeLock.unlock();
            }
            synchronized (this) {
                notifyAll();
            }
        }
    }

    /**
     * Process messages sequentially until all processing is complete
     *
     * @param function Logical method of processing messages in a loop
     */
    public void handleToLast(Function<Message, MessageProcessStatus> function) {
        log.debug("The message pipe：{} monitor thread is woken up, handing all message.", name);
        runningHandleAll = true;
        RLock takeLock = redissonClient.getLock(takeLockName);
        Message current = null;
        MessageProcessStatus status = MessageProcessStatus.SEND_SUCCESS;
        try {
            MessagePipeConfiguration.LockTime lockTime = configuration.getLockTime();
            if (takeLock.tryLock(lockTime.getWaitTime(), lockTime.getLeaseTime(), lockTime.getTimeUnit())) {
                while (queue.size() > 0) {
                    // Take first message
                    current = this.peek();
                    if (ObjectUtils.isEmpty(current)) {
                        break;
                    }
                    status = function.apply(current);

                    // Enhanced decision logic (same as handleFirst)
                    switch (status) {
                        case SEND_SUCCESS:
                            // Message sent successfully - remove from queue
                            this.poll();
                            recordSuccess(current);
                            break;

                        case SEND_EXCEPTION:
                            // Message failed - check if should retry
                            handleMessageFailure(current);
                            break;  // Exit loop to avoid continuous failures

                        case NO_HEALTH_CLIENT:
                            // No healthy client - don't count as failure
                            log.error("No healthy client available, will retry later: {}",
                                    new String(current.getBody()));
                            // DO NOT poll() - keep message in queue
                            break;  // Exit loop
                    }

                    // Set last process time
                    lastProcessTimeMillis.set(System.currentTimeMillis());
                }
            }
        } catch (Exception e) {
            this.doHandleException(e, status, current);
        } finally {
            runningHandleAll = false;
            if (takeLock.isLocked() && takeLock.isHeldByCurrentThread()) {
                takeLock.unlock();
            }
            synchronized (this) {
                notifyAll();
            }
        }
    }

    /**
     * Retrieves, but does not remove, the head of this queue,
     * or returns {@code null} if this queue is empty.
     *
     * @return the head of this queue, or {@code null} if this queue is empty
     */
    public Message peek() {
        Message message = null;
        if (!this.checkClientIsShutdown()) {
            message = queue.peek();
        }
        return message;
    }

    /**
     * Retrieves and removes the head of this queue,
     * or returns {@code null} if this queue is empty.
     *
     * @return the head of this queue, or {@code null} if this queue is empty
     */
    public Message poll() {
        Message message = null;
        if (!this.checkClientIsShutdown()) {
            message = queue.poll();
        }
        return message;
    }

    /**
     * Get the number of current messages in the pipeline
     *
     * @return count of message
     */
    public int size() {
        int messageSize = 0;
        if (!this.checkClientIsShutdown()) {
            messageSize = this.queue.size();
        }
        return messageSize;
    }

    /**
     * Get last invoke {@link #handleFirst}、{@link #handleToLast} method time millis
     *
     * @return Last call time，{@link java.util.concurrent.TimeUnit#MILLISECONDS}
     */
    public Long getLastProcessTimeMillis() {
        return this.lastProcessTimeMillis.get();
    }

    /**
     * Check whether the redisson client has been shutdown
     *
     * @return When it returns true, it means it has been shutdown
     */
    private boolean checkClientIsShutdown() {
        return redissonClient.isShutdown() || redissonClient.isShuttingDown();
    }

    /**
     * Execution processing exception
     *
     * @param e The {@link Exception} instance
     * @param current {@link Message} instance being processed
     */
    private void doHandleException(Exception e, MessageProcessStatus status, Message current) {
        if (e instanceof InterruptedException) {
            Thread.currentThread().interrupt();
        }
        ExceptionHandler exceptionHandler = configuration.getExceptionHandler();
        exceptionHandler.handleException(e, status, current);
    }

    /**
     * Check the status of message sending
     *
     * @param message Sent message instance {@link Message}
     */
    private void checkMessageSendStatus(Message message, MessageProcessStatus status) {
        switch (status) {
            case NO_HEALTH_CLIENT:
                throw new MessagePipeException("Message Pipe [" + name + "], no healthy clients were found，cancel send current message, content："
                        + new String(message.getBody()));
            case SEND_EXCEPTION:
                throw new MessagePipeException("MessagePipe [" + name + "] , handle message exception, message content: " +
                        new String(message.getBody()));
            case SEND_SUCCESS:
                log.debug("The message [{}] send successfully.", new String(message.getBody()));
                break;
        }
    }

    /**
     * Handle message failure - retry or move to DLQ
     * <p>
     * This method encapsulates the common retry logic used in both handleFirst() and handleToLast()
     * when a message fails to send. It checks if the message should be retried based on the
     * configured maximum retry attempts and exponential backoff delay.
     *
     * @param message the failed message
     */
    private void handleMessageFailure(Message message) {
        MessageRetryRecord record = getOrCreateRecord(message);
        record.setLastStatus(MessageResponseStatus.ERROR);

        if (record.shouldRetry()) {
            // Schedule retry with exponential backoff
            record.setRetryCount(record.getRetryCount() + 1);
            record.setLastRetryTime(System.currentTimeMillis());
            long delayMillis = record.getRetryDelayMillis();

            log.error("Message will be retried after {}ms (attempt {}/{}): {}",
                    delayMillis, record.getRetryCount(), record.getMaxRetries(),
                    new String(message.getBody()));

            messageRetryScheduler.scheduleRetry(message, delayMillis);
            updateRecord(message, record);
            // DO NOT poll() - keep message in queue for retry processing
        } else {
            // Max retries exceeded - move to DLQ
            log.error("Message max retries exceeded, moving to DLQ: {}",
                    new String(message.getBody()));

            messageDeadLetterQueue.send(message, record);
            this.poll();
            cleanupRecord(message);
        }
    }

    /**
     * Get or create a processing record for a message
     *
     * @param message the message to track
     * @return the MessageProcessingRecord (existing or newly created)
     */
    private MessageRetryRecord getOrCreateRecord(Message message) {
        String messageId = generateMessageId(message);

        RMap<String, MessageRetryRecord> recordMap =
                redissonClient.getMap(retryRecordsMapName);

        MessageRetryRecord record = recordMap.computeIfAbsent(messageId, k -> {
            MessageRetryRecord newRecord = MessageRetryRecord.of(messageId, message);
            // Set TTL on the entire map to automatically expire retry records after configured duration
            // This ensures old retry records are cleaned up automatically without manual intervention
            try {
                recordMap.expire(Duration.ofSeconds(configuration.getRetryRecordExpireSeconds()));
            } catch (Exception e) {
                log.debug("Failed to set TTL on retry records map: {}", retryRecordsMapName, e);
            }
            return newRecord;
        });

        return record;
    }

    /**
     * Update a retry record
     *
     * @param message the message
     * @param record the updated record
     */
    private void updateRecord(Message message, MessageRetryRecord record) {
        String messageId = generateMessageId(message);

        RMap<String, MessageRetryRecord> recordMap =
                redissonClient.getMap(retryRecordsMapName);

        recordMap.put(messageId, record);
    }

    /**
     * Record successful message processing
     *
     * @param message the successfully processed message
     */
    private void recordSuccess(Message message) {
        String messageId = generateMessageId(message);

        RMap<String, MessageRetryRecord> recordMap =
                redissonClient.getMap(retryRecordsMapName);

        recordMap.remove(messageId);
        log.debug("Message processed successfully: messageId={}", messageId);
    }

    /**
     * Clean up the retry record (used when moving to DLQ)
     *
     * @param message the message being cleaned up
     */
    private void cleanupRecord(Message message) {
        String messageId = generateMessageId(message);

        RMap<String, MessageRetryRecord> recordMap =
                redissonClient.getMap(retryRecordsMapName);

        recordMap.remove(messageId);
        log.debug("Retry record cleaned up: messageId={}", messageId);
    }

    /**
     * Generate a unique ID for a message
     * <p>
     * Uses UUID to ensure global uniqueness and avoid message content collisions.
     * This guarantees that identical messages get different IDs, preventing retry record overwrites.
     *
     * @param message the message
     * @return unique message identifier
     */
    private String generateMessageId(Message message) {
        return UUID.randomUUID().toString();
    }
}
