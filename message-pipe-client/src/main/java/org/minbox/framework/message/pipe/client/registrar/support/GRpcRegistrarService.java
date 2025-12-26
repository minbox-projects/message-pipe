package org.minbox.framework.message.pipe.client.registrar.support;

import com.google.common.util.concurrent.ListenableFuture;
import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import org.minbox.framework.message.pipe.client.ServerManager;
import org.minbox.framework.message.pipe.client.config.ClientConfiguration;
import org.minbox.framework.message.pipe.client.process.MessageProcessorManager;
import org.minbox.framework.message.pipe.client.registrar.RegistrarService;
import org.minbox.framework.message.pipe.core.exception.MessagePipeException;
import org.minbox.framework.message.pipe.core.grpc.ClientServiceGrpc;
import org.minbox.framework.message.pipe.core.grpc.proto.ClientHeartBeatRequest;
import org.minbox.framework.message.pipe.core.grpc.proto.ClientRegisterRequest;
import org.minbox.framework.message.pipe.core.grpc.proto.ClientResponse;
import org.minbox.framework.message.pipe.core.thread.MessagePipeThreadFactory;
import org.minbox.framework.message.pipe.core.transport.ClientHeartBeatResponseBody;
import org.minbox.framework.message.pipe.core.transport.ClientRegisterResponseBody;
import org.minbox.framework.message.pipe.core.transport.MessageResponseStatus;
import org.minbox.framework.message.pipe.core.untis.JsonUtils;
import org.springframework.util.ObjectUtils;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Register to Server in Grpc mode
 *
 * @author 恒宇少年
 */
@Slf4j
public class GRpcRegistrarService implements RegistrarService {
    private final ClientConfiguration configuration;
    private static final String THREAD_NAME_PREFIX = "heartbeat";
    private final ScheduledExecutorService heartBeatExecutorService;
    private final String pipeNames;

    private final AtomicBoolean isHeartBeatStarted = new AtomicBoolean(false);

    public GRpcRegistrarService(ClientConfiguration configuration, MessageProcessorManager messageProcessorManager) {
        this.configuration = configuration;
        this.pipeNames = messageProcessorManager.getBindingPipeNameString();
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
     * and when the number of retries reaches the upper limit retry register times,
     * terminate the registration
     */
    @Override
    public void register(String serverAddress, int serverPort) {
        boolean unregister = false;
        int maxTimes = configuration.getRetryRegisterTimes();
        int currentTimes = 0;
        while (!unregister) {
            try {
                String serverId = ServerManager.putIfNotPresent(configuration.getServerAddress(), configuration.getServerPort());
                ManagedChannel channel = ServerManager.establishChannel(serverId);
                ClientServiceGrpc.ClientServiceFutureStub stub =
                        ClientServiceGrpc.newFutureStub(channel);
                ClientRegisterRequest request = ClientRegisterRequest.newBuilder()
                        .setAddress(configuration.getLocalHost())
                        .setPort(configuration.getLocalPort())
                        .setMessagePipeName(pipeNames)
                        .build();
                ListenableFuture<ClientResponse> listenableFuture = stub.register(request);
                String responseJsonBody = listenableFuture.get().getBody();
                ClientRegisterResponseBody responseBody = JsonUtils.jsonToObject(responseJsonBody, ClientRegisterResponseBody.class);
                if (MessageResponseStatus.SUCCESS.equals(responseBody.getStatus())) {
                    // Start a heartbeat after successful registration
                    this.heartBeat();
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
        if (isHeartBeatStarted.compareAndSet(false, true)) {
            heartBeatExecutorService.scheduleAtFixedRate(() -> {
                String serverId = ServerManager.getServerId(configuration.getServerAddress(), configuration.getServerPort());
                try {
                    ManagedChannel channel = ServerManager.establishChannel(serverId);
                    ClientServiceGrpc.ClientServiceBlockingStub stub =
                            ClientServiceGrpc.newBlockingStub(channel);
                    ClientHeartBeatRequest request = ClientHeartBeatRequest.newBuilder()
                            .setAddress(configuration.getLocalHost())
                            .setPort(configuration.getLocalPort())
                            .build();
                    ClientResponse response = stub.heartbeat(request);
                    String responseJsonBody = response.getBody();
                    ClientHeartBeatResponseBody responseBody = JsonUtils.jsonToObject(responseJsonBody, ClientHeartBeatResponseBody.class);
                    if (MessageResponseStatus.ERROR.equals(responseBody.getStatus())) {
                        log.warn("The client is not registered with the server, re-registration is in progress...");
                        this.register(configuration.getServerAddress(), configuration.getServerPort());
                    }
                } catch (StatusRuntimeException e) {
                    Status.Code code = e.getStatus().getCode();
                    log.error("Send a heartbeat check exception to Server: {}, Status Code: {}", serverId, code);
                    // The server status is UNAVAILABLE
                    if (Status.Code.UNAVAILABLE == code) {
                        ServerManager.removeChannel(serverId);
                        log.error("The service is unavailable, and the cached channel is deleted.");
                    }
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            }, 5, configuration.getHeartBeatIntervalSeconds(), TimeUnit.SECONDS);
            log.info("Client heartBeat thread starting successfully，interval：{}，interval timeunit：{}.",
                    configuration.getHeartBeatIntervalSeconds(), TimeUnit.SECONDS);
        }
    }
}
