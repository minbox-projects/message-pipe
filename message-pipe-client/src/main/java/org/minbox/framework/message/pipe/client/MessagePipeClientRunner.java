package org.minbox.framework.message.pipe.client;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import lombok.extern.slf4j.Slf4j;
import org.minbox.framework.message.pipe.client.config.ClientConfiguration;
import org.minbox.framework.message.pipe.client.registrar.RegistrarService;
import org.minbox.framework.message.pipe.core.exception.MessagePipeException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The message pipe client server
 * <p>
 * Provide message processing service listener
 *
 * @author 恒宇少年
 * @see ReceiveMessageService
 */
@Slf4j
public class MessagePipeClientRunner implements InitializingBean, DisposableBean {
    /**
     * The bean name of {@link MessagePipeClientRunner}
     */
    public static final String BEAN_NAME = "messagePipeClientRunner";
    private static final ExecutorService RPC_MESSAGE_EXECUTOR = Executors.newFixedThreadPool(1);
    /**
     * The grpc server instance
     */
    private Server rpcServer;
    /**
     * Bound service interface instance
     *
     * @see ReceiveMessageService
     */
    private BindableService bindableService;
    /**
     * The client configuration
     */
    private ClientConfiguration configuration;

    /**
     * The {@link RegistrarService} instance
     *
     * @see org.minbox.framework.message.pipe.client.registrar.support.GRpcRegistrarService
     * @see org.minbox.framework.message.pipe.client.registrar.support.NacosRegistrarService
     */
    private RegistrarService registrarService;

    public MessagePipeClientRunner(ClientConfiguration configuration,
                                   ReceiveMessageService receiveMessageService,
                                   RegistrarService registrarService) {
        if (configuration.getLocalPort() <= 0 || configuration.getLocalPort() > 65535) {
            throw new MessagePipeException("MessagePipe Client port must be greater than 0 and less than 65535");
        }
        this.configuration = configuration;
        this.bindableService = receiveMessageService;
        this.registrarService = registrarService;
    }

    /**
     * Build the grpc {@link Server} instance
     */
    private void buildServer() {
        this.rpcServer = ServerBuilder
                .forPort(configuration.getLocalPort())
                .addService(this.bindableService)
                .build();
    }

    /**
     * Startup grpc {@link Server}
     */
    public void startup() {
        try {
            this.rpcServer.start();
            log.info("MessagePipe Client bind port : {}, startup successfully.", configuration.getLocalPort());
            this.rpcServer.awaitTermination();
        } catch (Exception e) {
            log.error("MessagePipe Client startup failed.", e);
        }
    }

    /**
     * Shutdown grpc {@link Server}
     */
    public void shutdown() {
        try {
            log.info("MessagePipe Client shutting down.");
            this.rpcServer.shutdown();
            long waitTime = 100;
            long timeConsuming = 0;
            while (!this.rpcServer.isShutdown()) {
                log.info("MessagePipe Client stopping....，total time consuming：{}", timeConsuming);
                timeConsuming += waitTime;
                Thread.sleep(waitTime);
            }
            log.info("MessagePipe Client stop successfully.");
        } catch (Exception e) {
            log.error("MessagePipe Client shutdown failed.", e);
        }
    }

    @Override
    public void destroy() throws Exception {
        this.shutdown();
    }

    /**
     * Register current client to server
     * <p>
     * Use child threads for registration logic
     *
     * @see ClientConfiguration
     */
    private void registerToServer() {
        Thread thread = new Thread(() ->
                this.registrarService.register(configuration.getServerAddress(), configuration.getServerPort()));
        thread.start();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        this.buildServer();
        this.registerToServer();
        // Starting Message process server
        RPC_MESSAGE_EXECUTOR.submit(() -> this.startup());
    }
}
