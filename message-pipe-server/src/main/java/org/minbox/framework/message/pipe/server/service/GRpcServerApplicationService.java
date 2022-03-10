package org.minbox.framework.message.pipe.server.service;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.minbox.framework.message.pipe.core.ClientStatus;
import org.minbox.framework.message.pipe.core.exception.MessagePipeException;
import org.minbox.framework.message.pipe.core.grpc.ClientServiceGrpc;
import org.minbox.framework.message.pipe.core.grpc.proto.ClientHeartBeatRequest;
import org.minbox.framework.message.pipe.core.grpc.proto.ClientRegisterRequest;
import org.minbox.framework.message.pipe.core.grpc.proto.ClientResponse;
import org.minbox.framework.message.pipe.core.information.ClientInformation;
import org.minbox.framework.message.pipe.core.transport.ClientHeartBeatResponseBody;
import org.minbox.framework.message.pipe.core.transport.ClientRegisterResponseBody;
import org.minbox.framework.message.pipe.core.transport.MessageResponseStatus;
import org.minbox.framework.message.pipe.core.untis.JsonUtils;
import org.minbox.framework.message.pipe.core.untis.StringUtils;
import org.minbox.framework.message.pipe.server.MessagePipe;
import org.minbox.framework.message.pipe.server.config.ServerConfiguration;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * The {@link MessagePipe} server application
 * <p>
 * Start some services required by the server
 *
 * @author 恒宇少年
 */
@Slf4j
public class GRpcServerApplicationService extends ClientServiceGrpc.ClientServiceImplBase
        implements InitializingBean, DisposableBean {
    private ScheduledExecutorService expiredScheduledExecutor;
    private Server rpcServer;
    private ServerConfiguration configuration;
    private ApplicationEventPublisher applicationEventPublisher;

    public GRpcServerApplicationService(ServerConfiguration configuration, ApplicationEventPublisher applicationEventPublisher) {
        if (configuration.getServerPort() <= 0 || configuration.getServerPort() > 65535) {
            throw new MessagePipeException("MessageServer port must be greater than 0 and less than 65535");
        }
        this.configuration = configuration;
        this.applicationEventPublisher = applicationEventPublisher;
        this.rpcServer = ServerBuilder.forPort(this.configuration.getServerPort()).addService(this).build();
        this.expiredScheduledExecutor = Executors.newScheduledThreadPool(configuration.getExpiredPoolSize());
    }

    /**
     * Register client
     *
     * @param request          client register request {@link ClientRegisterRequest}
     * @param responseObserver stream responseThreadPoolTaskExecutor
     */
    @Override
    public void register(ClientRegisterRequest request, StreamObserver<ClientResponse> responseObserver) {
        ClientRegisterResponseBody responseBody = new ClientRegisterResponseBody();
        try {
            if (StringUtils.isEmpty(request.getAddress()) || StringUtils.isEmpty(request.getMessagePipeName()) ||
                    (request.getPort() <= 0 || request.getPort() > 65535)) {
                throw new MessagePipeException("The client information verification fails and the registration cannot be completed.");
            }
            log.info("Registering client, IP: {}, Port: {}, pipeNames: {}",
                    request.getAddress(), request.getPort(), request.getMessagePipeName());
            ClientInformation client =
                    ClientInformation.valueOf(request.getAddress(), request.getPort(), request.getMessagePipeName());
            String clientId = client.getClientId();
            responseBody.setClientId(clientId);

            // Publish ServiceEvent
            ServiceEvent serviceEvent = new ServiceEvent(this, ServiceEventType.REGISTER, Arrays.asList(client));
            applicationEventPublisher.publishEvent(serviceEvent);
        } catch (Exception e) {
            responseBody.setStatus(MessageResponseStatus.ERROR);
            log.error("Register client failed.", e);
        }
        String responseBodyJson = JsonUtils.objectToJson(responseBody);
        ClientResponse response = ClientResponse.newBuilder().setBody(responseBodyJson).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    /**
     * Heartbeat check
     *
     * @param request          client heartbeat check request {@link ClientHeartBeatRequest}
     * @param responseObserver stream response
     */
    @Override
    public void heartbeat(ClientHeartBeatRequest request, StreamObserver<ClientResponse> responseObserver) {
        ClientHeartBeatResponseBody responseBody = new ClientHeartBeatResponseBody();
        try {
            if (StringUtils.isEmpty(request.getAddress()) ||
                    (request.getPort() <= 0 || request.getPort() > 65535)) {
                throw new MessagePipeException("The client information that sent the heartbeat is incomplete, " +
                        "and the heartbeat check is ignored this time.");
            }
            ClientInformation client = ClientInformation.valueOf(request.getAddress(), request.getPort(), null);
            Long currentTime = System.currentTimeMillis();
            client.setLastReportTime(currentTime);
            client.setOnlineTime(currentTime);
            client.setStatus(ClientStatus.ON_LINE);

            // Publish heart beat event
            ServiceEvent serviceEvent = new ServiceEvent(this, ServiceEventType.HEART_BEAT, Arrays.asList(client));
            applicationEventPublisher.publishEvent(serviceEvent);
        } catch (Exception e) {
            responseBody.setStatus(MessageResponseStatus.ERROR);
            log.error("Heartbeat check failed.", e);
        }
        String responseBodyJson = JsonUtils.objectToJson(responseBody);
        ClientResponse response = ClientResponse.newBuilder().setBody(responseBodyJson).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    /**
     * Startup grpc {@link Server}
     */
    public void startup() {
        new Thread(() -> {
            try {
                this.rpcServer.start();
                log.info("MessagePipe Server bind port : {}, startup successfully.", this.configuration.getServerPort());
                this.rpcServer.awaitTermination();
            } catch (Exception e) {
                log.error("MessagePipe Server startup failed.", e);
            }
        }).start();
    }

    /**
     * Start eliminate expired client
     * <p>
     * If the client's last heartbeat time is greater than the timeout threshold,
     * the update status is performed
     */
    private void startEliminateExpiredClient() {
        this.expiredScheduledExecutor.scheduleAtFixedRate(() -> {
            // Publish expired event
            ServiceEvent serviceEvent = new ServiceEvent(this, ServiceEventType.EXPIRE);
            applicationEventPublisher.publishEvent(serviceEvent);
        }, 10, configuration.getCheckClientExpiredIntervalSeconds(), TimeUnit.SECONDS);
        log.info("Eliminate expired client thread starting，interval：{}，interval timeunit：{}.",
                configuration.getCheckClientExpiredIntervalSeconds(), TimeUnit.SECONDS);
    }

    /**
     * Shutdown Grpc {@link Server}
     */
    private void shutdownServerApplication() {
        try {
            log.info("MessagePipe Server shutting down.");
            this.rpcServer.shutdown();
            long waitTime = 100;
            long timeConsuming = 0;
            while (!this.rpcServer.isShutdown()) {
                log.info("MessagePipe Server stopping...，total time consuming：{}", timeConsuming);
                timeConsuming += waitTime;
                Thread.sleep(waitTime);
            }
            log.info("MessagePipe Server stop successfully.");
        } catch (Exception e) {
            log.error("MessagePipe Server shutdown failed.", e);
        }
    }

    @Override
    public void destroy() throws Exception {
        this.shutdownServerApplication();
        this.expiredScheduledExecutor.shutdown();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        this.startup();
        this.startEliminateExpiredClient();
        log.info("MessagePipe ClientExpiredExecutor successfully started.");
    }
}
