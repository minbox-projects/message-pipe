package org.minbox.framework.message.pipe.server;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.minbox.framework.message.pipe.core.Message;
import org.minbox.framework.message.pipe.core.PipeConstants;
import org.minbox.framework.message.pipe.core.exception.MessagePipeException;
import org.minbox.framework.message.pipe.core.information.ClientInformation;
import org.minbox.framework.message.pipe.core.transport.MessageResponseStatus;
import org.minbox.framework.message.pipe.server.config.LockNames;
import org.minbox.framework.message.pipe.server.config.MessagePipeConfiguration;
import org.minbox.framework.message.pipe.server.exception.ExceptionHandler;
import org.minbox.framework.message.pipe.server.manager.MessageDeadLetterQueue;
import org.minbox.framework.message.pipe.server.manager.MessageProcessStatus;
import org.minbox.framework.message.pipe.server.manager.MessageRetryRecord;
import org.minbox.framework.message.pipe.server.service.discovery.ServiceDiscovery;
import org.redisson.api.*;
import org.springframework.util.ObjectUtils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.function.Supplier;

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
    private final String name;
    /**
     * The Redis blocking queue bound to the current message pipeline
     */
    @Getter
    private final RBlockingQueue<Message> queue;
    /**
     * The redisson client instance
     *
     * @see RBlockingQueue
     * @see RLock
     */
    @Getter
    private final RedissonClient redissonClient;
    /**
     * The queue name in redis
     */
    private final String queueName;
    /**
     * The retry records map name in redis
     * <p>
     * Format: {pipeName}_retry_records
     */
    private final String retryRecordsMapName;
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
     * The last time a "no healthy client" log was printed
     */
    private final AtomicLong lastNoHealthyClientLogTime = new AtomicLong(0);
    /**
     * Total input messages count
     */
    @Getter
    private final AtomicLong totalInputCount = new AtomicLong(0);
    /**
     * Total processed messages count
     */
    @Getter
    private final AtomicLong totalProcessCount = new AtomicLong(0);
    /**
     * The {@link MessagePipe} configuration
     */
    @Getter
    private final MessagePipeConfiguration configuration;
    /**
     * Schedule threads that process all data in the message pipeline regularly
     */
    @Getter
    private boolean isStopSchedulerThread;
    /**
     * Dead letter queue manager
     */
    @Getter
    private final MessageDeadLetterQueue messageDeadLetterQueue;
    /**
     * The service discovery
     */
    private final ServiceDiscovery serviceDiscovery;


    /**
     * Retry queue name format: {pipeName}_retry
     */
    private static final String RETRY_RECORDS_QUEUE_NAME_FORMAT = "%s_retry_records";


    public MessagePipe(String name,
                       RedissonClient redissonClient,
                       MessagePipeConfiguration configuration,
                       ServiceDiscovery serviceDiscovery) {
        this.name = name;
        this.queueName = LockNames.MESSAGE_QUEUE.format(this.name);
        this.retryRecordsMapName = String.format(RETRY_RECORDS_QUEUE_NAME_FORMAT, this.name);
        this.putLockName = LockNames.PUT_MESSAGE.format(this.name);
        this.takeLockName = LockNames.TAKE_MESSAGE.format(this.name);
        this.redissonClient = redissonClient;
        this.configuration = configuration;
        this.serviceDiscovery = serviceDiscovery;
        this.queue = redissonClient.getBlockingQueue(this.queueName, configuration.getCodec());

        // Initialize DLQ
        this.messageDeadLetterQueue = new MessageDeadLetterQueue(redissonClient, name, configuration);

        if (this.name == null || this.name.trim().isEmpty()) {
            throw new MessagePipeException("The MessagePipe name is required，cannot be null.");
        }
        if (this.redissonClient == null) {
            throw new MessagePipeException("The RedissonClient cannot be null.");
        }
        if (this.configuration == null) {
            throw new MessagePipeException("The MessagePipeConfiguration cannot be null.");
        }
        if (this.serviceDiscovery == null) {
            throw new MessagePipeException("The ServiceDiscovery cannot be null.");
        }
    }

    /**
     * put message to current {@link MessagePipe} with {@link RLock}
     *
     * @param message The {@link Message} instance
     */
    public void putLastOnLock(Message message) {
        RLock putLock = redissonClient.getLock(putLockName);
        try {
            MessagePipeConfiguration.LockTime lockTime = configuration.getPutLockTime();
            long leaseTime = lockTime.getLeaseTime();
            boolean isLocked;
            if (leaseTime == -1) {
                isLocked = putLock.tryLock(lockTime.getWaitTime(), lockTime.getTimeUnit());
            } else {
                isLocked = putLock.tryLock(lockTime.getWaitTime(), leaseTime, lockTime.getTimeUnit());
            }
            if (isLocked) {
                boolean addSuccess = queue.offer(message);
                if (!addSuccess) {
                    throw new MessagePipeException("Unsuccessful when writing the message to the queue.");
                }
                totalInputCount.incrementAndGet();
            }
        } catch (Exception e) {
            this.doHandleException(e, MessageProcessStatus.PUT_EXCEPTION, message);
        } finally {
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
        try {
            boolean addSuccess = queue.offer(message);
            if (!addSuccess) {
                throw new MessagePipeException("Unsuccessful when writing the message to the queue.");
            }
            totalInputCount.incrementAndGet();
        } catch (Exception e) {
            this.doHandleException(e, MessageProcessStatus.PUT_EXCEPTION, message);
        } finally {
            synchronized (this) {
                notifyAll();
            }
        }
    }

    /**
     * put batch message to current {@link MessagePipe} with {@link RLock}
     *
     * @param messages The {@link Message} list
     */
    public void putLastBatchOnLock(List<Message> messages) {
        if (ObjectUtils.isEmpty(messages)) {
            return;
        }
        RLock putLock = redissonClient.getLock(putLockName);
        try {
            MessagePipeConfiguration.LockTime lockTime = configuration.getPutLockTime();
            long leaseTime = lockTime.getLeaseTime();
            boolean isLocked;
            if (leaseTime == -1) {
                isLocked = putLock.tryLock(lockTime.getWaitTime(), lockTime.getTimeUnit());
            } else {
                isLocked = putLock.tryLock(lockTime.getWaitTime(), leaseTime, lockTime.getTimeUnit());
            }
            if (isLocked) {
                // Split large batch into smaller chunks to avoid WriteRedisConnectionException
                int batchSize = configuration.getPutBatchSize();
                for (int i = 0; i < messages.size(); i += batchSize) {
                    int end = Math.min(messages.size(), i + batchSize);
                    List<Message> subList = messages.subList(i, end);
                    boolean addSuccess = queue.addAll(subList);
                    if (!addSuccess) {
                        throw new MessagePipeException("Unsuccessful when writing the batch messages to the queue.");
                    }
                }
                totalInputCount.addAndGet(messages.size());
            }
        } catch (Exception e) {
            for (Message message : messages) {
                this.doHandleException(e, MessageProcessStatus.PUT_EXCEPTION, message);
            }
        } finally {
            if (putLock.isLocked() && putLock.isHeldByCurrentThread()) {
                putLock.unlock();
            }
            synchronized (this) {
                notifyAll();
            }
        }
    }

    /**
     * put batch message to current {@link MessagePipe}
     *
     * @param messages The {@link Message} list
     */
    public void putLastBatch(List<Message> messages) {
        if (ObjectUtils.isEmpty(messages)) {
            return;
        }
        log.debug("write the batch new message, size：{}.", messages.size());
        try {
            // Split large batch into smaller chunks to avoid WriteRedisConnectionException
            int batchSize = configuration.getPutBatchSize();
            for (int i = 0; i < messages.size(); i += batchSize) {
                int end = Math.min(messages.size(), i + batchSize);
                List<Message> subList = messages.subList(i, end);
                boolean addSuccess = queue.addAll(subList);
                if (!addSuccess) {
                    throw new MessagePipeException("Unsuccessful when writing the batch messages to the queue.");
                }
            }
            totalInputCount.addAndGet(messages.size());
        } catch (Exception e) {
            for (Message message : messages) {
                this.doHandleException(e, MessageProcessStatus.PUT_EXCEPTION, message);
            }
        } finally {
            synchronized (this) {
                notifyAll();
            }
        }
    }

    /**
     * Process messages sequentially until all processing is complete
     *
     * @param batchSender Logical method of processing a batch of messages
     * @param clientSupplier Supplier to resolve client for current pipe
     * @return true if lock was acquired, false otherwise
     */
    public boolean handleToLast(Function<List<Message>, Integer> batchSender,
                                Supplier<ClientInformation> clientSupplier) {
        log.debug("The message pipe：{} is handing all message.", name);
        RLock takeLock = redissonClient.getLock(takeLockName);
        int batchSize = configuration.getBatchSize();

        try {
            MessagePipeConfiguration.LockTime lockTime = configuration.getTakeLockTime();
            long leaseTime = lockTime.getLeaseTime();
            boolean isLocked;
            if (leaseTime == -1) {
                isLocked = takeLock.tryLock(lockTime.getWaitTime(), lockTime.getTimeUnit());
            } else {
                isLocked = takeLock.tryLock(lockTime.getWaitTime(), leaseTime, lockTime.getTimeUnit());
            }
            if (isLocked) {
                try {
                    RList<Message> rList = (RList<Message>) queue;
                    while (true) {
                        // 1. Batch fetch messages
                        List<Message> batchMessages = rList.range(0, batchSize - 1);

                        if (ObjectUtils.isEmpty(batchMessages)) {
                            break;
                        }

                        // 2. Check client availability (Lightweight check before heavy lifting)
                        ClientInformation clientCheck = clientSupplier.get();
                        if (ObjectUtils.isEmpty(clientCheck)) {
                            long currentTime = System.currentTimeMillis();
                            if (currentTime - lastNoHealthyClientLogTime.get() > 10000) {
                                log.error("Message Pipe [{}], No healthy client available, will retry later: {}",
                                        this.name, new String(batchMessages.get(0).getBody()));
                                lastNoHealthyClientLogTime.set(currentTime);
                            }
                            break; // Wait for next cycle
                        }

                        // 3. Batch Send via gRPC
                        // Returns the number of successfully processed messages
                        int successCount = batchSender.apply(batchMessages);

                        // Track processed message IDs for logging in case of lock loss
                        List<String> processedMessageIds = new ArrayList<>();

                        // 4. Record successes (for internal tracking/metrics)
                        if (successCount > 0) {
                            for (int i = 0; i < successCount; i++) {
                                Message msg = batchMessages.get(i);
                                String msgId = msg.getMessageId();
                                if (msgId != null) {
                                    processedMessageIds.add(msgId);
                                }
                            }
                            // Batch remove retry records
                            this.recordSuccessBatch(processedMessageIds);

                            // Increment total processed count
                            totalProcessCount.addAndGet(successCount);
                        }

                        // 5. Batch delete processed messages from Redis
                        if (successCount > 0) {
                            // CRITICAL: Ensure we still hold the lock before deleting data
                            if (takeLock.isLocked() && takeLock.isHeldByCurrentThread()) {
                                // Remove the first 'successCount' messages
                                rList.trim(successCount, -1);
                                log.debug("Message Pipe [{}], Batch processed and removed {} messages.", name, successCount);

                                // Log each successfully processed messageId individually after trim
                                processedMessageIds.forEach(msgId -> log.info("The message [{}] send successfully.", msgId));
                            } else {
                                log.warn("Message Pipe [{}], Lock lost during batch processing! Skipping delete to prevent data loss. " +
                                        "The following {} messages were sent but not deleted and WILL BE RE-PROCESSED: {}", name, processedMessageIds.size(), processedMessageIds);
                                processedMessageIds.clear();
                                break; // Break loop to re-acquire lock
                            }
                        }

                        // 6. Handle failure if batch was interrupted (Partial or Total failure)
                        if (successCount < batchMessages.size()) {
                            // successCount == -1 means connection/network error.
                            // We should NOT increment retry count or move to DLQ for network issues.
                            // Just break the loop to retry later (infinite retry until connected).
                            if (successCount == -1) {
                                log.error("Message Pipe [{}], Network/Connection error when sending batch. Will retry later.", name);
                            } else {
                                // successCount >= 0 means Client received batch but processed partially.
                                // The message at 'successCount' index is the one that failed business logic.
                                int firstFailedIndex = successCount;
                                if (firstFailedIndex < batchMessages.size()) {
                                    Message failedMessage = batchMessages.get(firstFailedIndex);
                                    handleMessageFailure(failedMessage);
                                }
                            }
                            // Break outer loop to wait/retry
                            break;
                        }

                        // Set last process time
                        lastProcessTimeMillis.set(System.currentTimeMillis());
                    }
                } finally {
                    if (takeLock.isLocked() && takeLock.isHeldByCurrentThread()) {
                        takeLock.unlock();
                    }
                    synchronized (this) {
                        notifyAll();
                    }
                }
                return true;
            }
        } catch (Exception e) {
            this.doHandleException(e, MessageProcessStatus.SEND_EXCEPTION, null);
        }
        return false;
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
     * Get last invoke {@link #handleToLast} method time millis
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
        // Check if the exception or any of its causes is InterruptedException
        Throwable cause = e;
        while (cause != null) {
            if (cause instanceof InterruptedException) {
                Thread.currentThread().interrupt();
                break;
            }
            cause = cause.getCause();
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
     * This method encapsulates the common retry logic used in handleToLast()
     * when a message fails to send. It checks if the message should be retried based on the
     * configured maximum retry attempts and exponential backoff delay.
     *
     * @param message the failed message
     */
    private void handleMessageFailure(Message message) {
        if (!serviceDiscovery.checkHaveHealthClient(this.name)) {
            return;
        }
        MessageRetryRecord record = getOrCreateRecord(message);
        record.setLastStatus(MessageResponseStatus.ERROR);

        if (record.shouldRetry()) {
            // Increment retry count and update record
            record.setRetryCount(record.getRetryCount() + 1);
            record.setLastRetryTime(System.currentTimeMillis());
            long delayMillis = record.getRetryDelayMillis();

            log.error("Message Pipe [{}]，Message will be retried after {}ms (attempt {}/{}): {}",
                    this.name, delayMillis, record.getRetryCount(), record.getMaxRetries(),
                    new String(message.getBody()));

            updateRecord(message, record);

            // Wait in current thread, keep message in queue head
            try {
                Thread.sleep(delayMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        } else {
            // Max retries exceeded - move to DLQ
            log.error("Message Pipe [{}]，Message max retries exceeded, moving to DLQ: {}",
                    this.name, new String(message.getBody()));

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
        String messageId = message.getMessageId();

        RMap<String, MessageRetryRecord> recordMap =
                redissonClient.getMap(retryRecordsMapName, configuration.getCodec());

        MessageRetryRecord record = recordMap.computeIfAbsent(messageId, k -> {
            MessageRetryRecord newRecord = MessageRetryRecord.of(messageId, message);
            // Set TTL on the entire map to automatically expire retry records after configured duration
            try {
                recordMap.expire(configuration.getRetryRecordExpireSeconds(), TimeUnit.SECONDS);
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
        String messageId = message.getMessageId();

        RMap<String, MessageRetryRecord> recordMap =
                redissonClient.getMap(retryRecordsMapName, configuration.getCodec());

        recordMap.put(messageId, record);
    }

    /**
     * Batch record successful message processing (remove retry records)
     *
     * @param messageIds the list of successfully processed message IDs
     */
    private void recordSuccessBatch(java.util.List<String> messageIds) {
        if (ObjectUtils.isEmpty(messageIds)) {
            return;
        }

        RMap<String, MessageRetryRecord> recordMap =
                redissonClient.getMap(retryRecordsMapName, configuration.getCodec());

        recordMap.fastRemove(messageIds.toArray(new String[0]));
    }

    /**
     * Clean up the retry record (used when moving to DLQ)
     *
     * @param message the message being cleaned up
     */
    private void cleanupRecord(Message message) {
        String messageId = message.getMessageId();

        RMap<String, MessageRetryRecord> recordMap =
                redissonClient.getMap(retryRecordsMapName, configuration.getCodec());

        recordMap.remove(messageId);
        log.debug("Retry record cleaned up: messageId={}", messageId);
    }


    /**
     * Set the stop scheduler thread flag
     * <p>
     * When the flag is set to true, the scheduler thread will stop.
     * notify all waiting threads to wake up and check the flag.
     *
     * @param stopSchedulerThread The stop flag
     */
    public void setStopSchedulerThread(boolean stopSchedulerThread) {
        this.isStopSchedulerThread = stopSchedulerThread;
        synchronized (this) {
            notifyAll();
        }
    }
}
