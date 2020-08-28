package org.minbox.framework.message.pipe.server.distribution;

import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import org.minbox.framework.message.pipe.core.Message;
import org.minbox.framework.message.pipe.core.grpc.MessageServiceGrpc;
import org.minbox.framework.message.pipe.core.grpc.proto.MessageRequest;
import org.minbox.framework.message.pipe.core.grpc.proto.MessageResponse;
import org.minbox.framework.message.pipe.core.information.ClientInformation;
import org.minbox.framework.message.pipe.core.thread.MessagePipeThreadFactory;
import org.minbox.framework.message.pipe.core.transport.MessageRequestBody;
import org.minbox.framework.message.pipe.core.transport.MessageResponseBody;
import org.minbox.framework.message.pipe.core.transport.MessageResponseStatus;
import org.minbox.framework.message.pipe.core.untis.JsonUtils;
import org.minbox.framework.message.pipe.server.LockNames;
import org.minbox.framework.message.pipe.server.MessagePipe;
import org.minbox.framework.message.pipe.server.config.MessagePipeConfiguration;
import org.minbox.framework.message.pipe.server.exception.ExceptionHandler;
import org.minbox.framework.message.pipe.server.service.discovery.ServiceDiscovery;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.util.ObjectUtils;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Execute messages in the distribution {@link MessagePipe}
 *
 * @author 恒宇少年
 */
@Slf4j
public class MessageDistributionExecutor {
    private String pipeName;
    private ScheduledExecutorService scheduledExecutorService;
    private RedissonClient redissonClient;
    private MessagePipeConfiguration configuration;
    private ServiceDiscovery serviceDiscovery;

    public MessageDistributionExecutor(String pipeName,
                                       RedissonClient redissonClient,
                                       MessagePipeConfiguration configuration,
                                       ServiceDiscovery serviceDiscovery) {
        this.pipeName = pipeName;
        this.redissonClient = redissonClient;
        this.configuration = configuration;
        this.serviceDiscovery = serviceDiscovery;
        this.scheduledExecutorService = Executors.newScheduledThreadPool(configuration.getDistributionMessagePoolSize(),
                new MessagePipeThreadFactory(this.pipeName));
    }

    /**
     * Waiting for new news
     * <p>
     * After discovering a new message from the message pipeline, perform distribution to the client
     */
    public void waitingForNewMessage() {
        scheduledExecutorService.scheduleWithFixedDelay(() -> {
                    try {
                        ClientInformation client = serviceDiscovery.lookup(this.pipeName);
                        if (!ObjectUtils.isEmpty(client) && !this.checkClientIsShutdown()) {
                            this.takeAndSend(client);
                        }
                    } catch (Exception e) {
                        log.error(e.getMessage(), e);
                    }
                },
                configuration.getDistributionMessageInitialDelay(),
                configuration.getDistributionMessageDelay(),
                configuration.getDistributionMessageTimeUnit());
    }


    /**
     * task a message
     * <p>
     * take and remove the first message from current {@link MessagePipe}
     *
     * @return The {@link Message} instance
     */
    private void takeAndSend(ClientInformation client) {
        Message message = null;
        String takeLockName = LockNames.TAKE_MESSAGE.format(this.pipeName);
        RLock takeLock = redissonClient.getLock(takeLockName);
        log.debug("lock:" + takeLock.toString() + ",interrupted:" + Thread.currentThread().isInterrupted()
                + ",hold:" + takeLock.isHeldByCurrentThread() + ",threadId:" + Thread.currentThread().getId());
        try {
            MessagePipeConfiguration.LockTime lockTime = configuration.getLockTime();
            if (takeLock.tryLock(lockTime.getWaitTime(), lockTime.getLeaseTime(), lockTime.getTimeUnit())) {
                log.debug("Thread：{}, acquired lock.", Thread.currentThread().getId());
                String queueLockName = LockNames.MESSAGE_QUEUE.format(this.pipeName);
                RBlockingQueue<Message> queue = redissonClient.getBlockingQueue(queueLockName);
                message = queue.peek();
                if (!ObjectUtils.isEmpty(message)) {
                    boolean isSendSuccessfully = this.sendMessageToClient(message, client);
                    if (isSendSuccessfully) {
                        queue.poll();
                    }
                }
            }
        } catch (Exception e) {
            ExceptionHandler exceptionHandler = this.configuration.getExceptionHandler();
            exceptionHandler.handleException(e, message);
        } finally {
            if (!this.checkClientIsShutdown() && takeLock.isLocked() && takeLock.isHeldByCurrentThread()) {
                takeLock.unlock();
            }
        }
    }

    /**
     * Send {@link Message} to client
     *
     * @param message The {@link Message} instance
     */
    private boolean sendMessageToClient(Message message, ClientInformation clientInformation) {
        boolean isSendSuccessfully = true;
        String clientId = clientInformation.getClientId();
        ManagedChannel channel = ClientChannelManager.establishChannel(clientInformation);
        try {
            MessageServiceGrpc.MessageServiceBlockingStub messageClientStub = MessageServiceGrpc.newBlockingStub(channel);
            String requestId = this.configuration.getRequestIdGenerator().generate();
            MessageRequestBody requestBody =
                    new MessageRequestBody()
                            .setRequestId(requestId)
                            .setClientId(clientId)
                            .setMessage(message)
                            .setPipeName(this.pipeName);
            String requestJsonBody = JsonUtils.objectToJson(requestBody);
            MessageResponse response = messageClientStub
                    .messageProcessing(MessageRequest.newBuilder().setBody(requestJsonBody).build());
            MessageResponseBody responseBody = JsonUtils.jsonToObject(response.getBody(), MessageResponseBody.class);
            if (!MessageResponseStatus.SUCCESS.equals(responseBody.getStatus())) {
                isSendSuccessfully = false;
                log.error("To the client: {}, " +
                        "the message is sent abnormally, and the message is recovered.", clientId);
            }
        } catch (StatusRuntimeException e) {
            isSendSuccessfully = false;
            Status.Code code = e.getStatus().getCode();
            log.error("To the client: {}, exception when sending a message, Status Code: {}", clientId, code);
            // The server status is UNAVAILABLE
            if (Status.Code.UNAVAILABLE == code) {
                ClientChannelManager.removeChannel(clientId);
                log.error("The client is unavailable, and the cached channel is deleted.");
            }
        } catch (Exception e) {
            throw e;
        }
        if (isSendSuccessfully) {
            log.debug("To the client: {}, sending the message is complete.", clientId);
        }
        return isSendSuccessfully;
    }

    /**
     * Check whether the redisson client has been shutdown
     *
     * @return When it returns true, it means it has been shutdown
     */
    private boolean checkClientIsShutdown() {
        return redissonClient.isShutdown() || redissonClient.isShuttingDown();
    }
}
