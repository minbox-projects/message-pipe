package org.minbox.framework.message.pipe.client.connect;

import com.alibaba.fastjson.JSON;
import com.google.common.util.concurrent.ListenableFuture;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.extern.slf4j.Slf4j;
import org.minbox.framework.message.pipe.client.config.ClientConfiguration;
import org.minbox.framework.message.pipe.core.exception.MessagePipeException;
import org.minbox.framework.message.pipe.core.grpc.ClientServiceGrpc;
import org.minbox.framework.message.pipe.core.grpc.proto.ClientHeartBeatRequest;
import org.minbox.framework.message.pipe.core.grpc.proto.ClientRegisterRequest;
import org.minbox.framework.message.pipe.core.grpc.proto.ClientResponse;
import org.minbox.framework.message.pipe.core.transport.ClientRegisterResponseBody;
import org.minbox.framework.message.pipe.core.transport.MessageResponseStatus;
import org.minbox.framework.message.pipe.core.untis.InternetAddressUtils;
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
    private static final String PIPE_NAME_SPLIT = ",";
    private ScheduledExecutorService heartBeatExecutorService;
    private ClientConfiguration configuration;

    public ConnectServerExecutor(ClientConfiguration configuration) {
        this.configuration = configuration;
        if (configuration.getServerPort() <= 0 || configuration.getServerPort() > 65535) {
            throw new MessagePipeException("MessagePipe Server port must be greater than 0 and less than 65535");
        }
        if (ObjectUtils.isEmpty(configuration.getServerAddress())) {
            throw new MessagePipeException("Registration target server address cannot be empty.");
        }
        if (ObjectUtils.isEmpty(configuration.getBindPipeNames())) {
            throw new MessagePipeException("At least one message pipe is bound.");
        }
        this.heartBeatExecutorService = Executors.newScheduledThreadPool(1);
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
                ManagedChannel channel = this.createChannel();
                ClientServiceGrpc.ClientServiceFutureStub stub =
                        ClientServiceGrpc.newFutureStub(channel);
                String pipeNames = StringUtils.arrayToDelimitedString(configuration.getBindPipeNames(), PIPE_NAME_SPLIT);
                String localHost = InternetAddressUtils.getLocalHost();
                ClientRegisterRequest request = ClientRegisterRequest.newBuilder()
                        .setAddress(localHost)
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
                channel.shutdown();
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
            ManagedChannel channel = this.createChannel();
            ClientServiceGrpc.ClientServiceFutureStub stub =
                    ClientServiceGrpc.newFutureStub(channel);
            String localHost = InternetAddressUtils.getLocalHost();
            ClientHeartBeatRequest request = ClientHeartBeatRequest.newBuilder()
                    .setAddress(localHost)
                    .setPort(this.configuration.getLocalPort())
                    .build();
            stub.heartbeat(request);
            channel.shutdown();
        }, 5, configuration.getHeartBeatIntervalSeconds(), TimeUnit.SECONDS);
    }

    /**
     * Create a channel to connect to Server
     *
     * @return {@link ManagedChannel} instance
     */
    private ManagedChannel createChannel() {
        return ManagedChannelBuilder
                .forAddress(configuration.getServerAddress(), configuration.getServerPort())
                .usePlaintext()
                .build();
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
