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
import org.minbox.framework.message.pipe.server.ClientManager;
import org.minbox.framework.message.pipe.server.LockNames;
import org.minbox.framework.message.pipe.server.MessagePipe;
import org.minbox.framework.message.pipe.server.config.MessagePipeConfiguration;
import org.minbox.framework.message.pipe.server.exception.ExceptionHandler;
import org.minbox.framework.message.pipe.server.lb.ClientLoadBalanceStrategy;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.util.ObjectUtils;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Execute messages in the distribution {@link MessagePipe}
 *
 * @author 恒宇少年
 */
@Slf4j
public class MessageDistributionExecutor {
    private ExecutorService executorService;
    private String pipeName;
    private RedissonClient redissonClient;
    private MessagePipeConfiguration configuration;

    public MessageDistributionExecutor(String pipeName, RedissonClient redissonClient, MessagePipeConfiguration configuration) {
        this.pipeName = pipeName;
        this.redissonClient = redissonClient;
        this.configuration = configuration;
        this.executorService = Executors.newFixedThreadPool(configuration.getDistributionMessagePoolSize(),
                new MessagePipeThreadFactory(this.pipeName));
    }

    /**
     * Waiting for new news
     * <p>
     * After discovering a new message from the message pipeline, perform distribution to the client
     */
    public void waitingForNewMessage() {
        executorService.submit(() -> {
            for (; ; ) {
                try {
                    List<ClientInformation> clients = ClientManager.getPipeBindOnLineClients(this.pipeName);
                    if (!ObjectUtils.isEmpty(clients) && !this.checkClientIsShutdown()) {
                        this.takeAndSend(clients);
                    }
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            }
        });
    }


    /**
     * task a message
     * <p>
     * take and remove the first message from current {@link MessagePipe}
     *
     * @return The {@link Message} instance
     */
    private void takeAndSend(List<ClientInformation> clients) {
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
                    ClientLoadBalanceStrategy strategy = this.configuration.getLoadBalanceStrategy();
                    ClientInformation clientInformation = strategy.lookup(clients);
                    boolean isSendSuccessfully = this.sendMessageToClient(message, clientInformation);
                    if (isSendSuccessfully) {
                        queue.poll();
                    }
                }
            }
        } catch (Exception e) {
            ExceptionHandler exceptionHandler = this.configuration.getExceptionHandler();
            exceptionHandler.handleException(e, message);
        } finally {
            if (takeLock.isLocked() && takeLock.isHeldByCurrentThread() && !this.checkClientIsShutdown()) {
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
        String clientId = ClientManager.getClientId(clientInformation.getAddress(), clientInformation.getPort());
        ManagedChannel channel = ClientManager.establishChannel(clientId);
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
                ClientManager.removeChannel(clientId);
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
