package org.minbox.framework.message.pipe.client.connect;

import com.alibaba.fastjson.JSON;
import com.google.common.util.concurrent.ListenableFuture;
import io.grpc.ManagedChannel;
import lombok.extern.slf4j.Slf4j;
import org.minbox.framework.message.pipe.client.ServerManager;
import org.minbox.framework.message.pipe.client.config.ClientConfiguration;
import org.minbox.framework.message.pipe.client.process.MessageProcessorManager;
import org.minbox.framework.message.pipe.core.exception.MessagePipeException;
import org.minbox.framework.message.pipe.core.grpc.ClientServiceGrpc;
import org.minbox.framework.message.pipe.core.grpc.proto.ClientHeartBeatRequest;
import org.minbox.framework.message.pipe.core.grpc.proto.ClientRegisterRequest;
import org.minbox.framework.message.pipe.core.grpc.proto.ClientResponse;
import org.minbox.framework.message.pipe.core.thread.MessagePipeThreadFactory;
import org.minbox.framework.message.pipe.core.transport.ClientRegisterResponseBody;
import org.minbox.framework.message.pipe.core.transport.MessageResponseStatus;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Connect to server executor
 * <p>
 * This class is registered to the server after the initialization is completed,
 * and the heartbeat sending is performed by the timing thread pool after the registration is successful
 *
 * @author 恒宇少年
 */
@Slf4j
public class ConnectServerExecutor implements InitializingBean {
    /**
     * The bean name of {@link ConnectServerExecutor}
     */
    public static final String BEAN_NAME = "connectServerExecutor";
    private static final String THREAD_NAME_PREFIX = "heartbeat";
    private static final String PIPE_NAME_SPLIT = ",";
    private ScheduledExecutorService heartBeatExecutorService;
    private ClientConfiguration configuration;
    private String[] pipeNames;

    public ConnectServerExecutor(ClientConfiguration configuration, MessageProcessorManager messageProcessorManager) {
        this.configuration = configuration;
        this.pipeNames = messageProcessorManager.getBindingPipeNames();
        if (configuration.getServerPort() <= 0 || configuration.getServerPort() > 65535) {
            throw new MessagePipeException("MessagePipe Server port must be greater than 0 and less than 65535");
        }
        if (ObjectUtils.isEmpty(configuration.getServerAddress())) {
            throw new MessagePipeException("Registration target server address cannot be empty.");
        }
        if (ObjectUtils.isEmpty(this.pipeNames)) {
            throw new MessagePipeException("At least one message pipe is bound.");
        }
        this.heartBeatExecutorService = Executors.newScheduledThreadPool(5,
                new MessagePipeThreadFactory(THREAD_NAME_PREFIX));
    }

    /**
     * Register client to server
     * <p>
     * If the connection fails, perform a retry,
     * and when the number of retries reaches the upper limit {@link ClientConfiguration#getRetryRegisterTimes()},
     * terminate the registration
     */
    private void register() {
        boolean unregister = false;
        int maxTimes = configuration.getRetryRegisterTimes();
        int currentTimes = 0;
        while (!unregister) {
            try {
                String serverId = ServerManager.putIfNotPresent(configuration.getServerAddress(), configuration.getServerPort());
                ManagedChannel channel = ServerManager.establishChannel(serverId);
                ClientServiceGrpc.ClientServiceFutureStub stub =
                        ClientServiceGrpc.newFutureStub(channel);
                String pipeNames = StringUtils.arrayToDelimitedString(this.pipeNames, PIPE_NAME_SPLIT);
                ClientRegisterRequest request = ClientRegisterRequest.newBuilder()
                        .setAddress(configuration.getLocalHost())
                        .setPort(configuration.getLocalPort())
                        .setMessagePipeName(pipeNames)
                        .build();
                ListenableFuture<ClientResponse> listenableFuture = stub.register(request);
                String responseJsonBody = listenableFuture.get().getBody();
                ClientRegisterResponseBody responseBody = JSON.parseObject(responseJsonBody, ClientRegisterResponseBody.class);
                if (MessageResponseStatus.SUCCESS.equals(responseBody.getStatus())) {
                    log.info("Registered to Server successfully, ClientId: {}", responseBody.getClientId());
                    unregister = true;
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
            if (!unregister) {
                currentTimes++;
                if (currentTimes > maxTimes) {
                    throw new MessagePipeException("The number of registration retries reaches the upper limit, " +
                            "the maximum number of times：" + configuration.getRetryRegisterTimes());
                }
                try {
                    Thread.sleep(configuration.getRetryRegisterIntervalMilliSeconds());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Send heart beat to server
     */
    private void heartBeat() {
        heartBeatExecutorService.scheduleAtFixedRate(() -> {
            try {
                String serverId = ServerManager.getServerId(configuration.getServerAddress(), configuration.getServerPort());
                ManagedChannel channel = ServerManager.establishChannel(serverId);
                ClientServiceGrpc.ClientServiceFutureStub stub =
                        ClientServiceGrpc.newFutureStub(channel);
                ClientHeartBeatRequest request = ClientHeartBeatRequest.newBuilder()
                        .setAddress(configuration.getLocalHost())
                        .setPort(configuration.getLocalPort())
                        .build();
                stub.heartbeat(request);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }, 5, configuration.getHeartBeatIntervalSeconds(), TimeUnit.SECONDS);
    }

    /**
     * After Bean initialization
     * <p>
     * execute registering client
     * send heartbeat regularly
     *
     * @throws Exception The exception instance
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        this.register();
        this.heartBeat();
    }
}
