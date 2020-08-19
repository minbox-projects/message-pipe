package org.minbox.framework.message.pipe.server;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import lombok.extern.slf4j.Slf4j;
import org.minbox.framework.message.pipe.server.config.ServerConfiguration;
import org.minbox.framework.message.pipe.server.exception.MessagePipeException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

/**
 * The {@link MessagePipe} server application
 * <p>
 * Start some services required by the server
 *
 * @author 恒宇少年
 */
@Slf4j
public class MessagePipeServerApplication implements InitializingBean, DisposableBean {
    /**
     * The bean name of {@link MessagePipeServerApplication}
     */
    public static final String BEAN_NAME = "messagePipeServerApplication";
    /**
     * The grpc server instance
     */
    private Server rpcServer;
    /**
     * Bound service interface instance
     *
     * @see ClientInteractiveService
     */
    private BindableService bindableService;
    /**
     * Server configuration
     */
    private ServerConfiguration configuration;

    public MessagePipeServerApplication(ServerConfiguration configuration, ClientInteractiveService clientInteractiveService) {
        if (configuration.getServerPort() <= 0 || configuration.getServerPort() > 65535) {
            throw new MessagePipeException("MessageServer port must be greater than 0 and less than 65535");
        }
        this.configuration = configuration;
        this.bindableService = clientInteractiveService;
    }

    /**
     * Build the grpc {@link Server} instance
     */
    private void buildServer() {
        this.rpcServer = ServerBuilder
                .forPort(this.configuration.getServerPort())
                .addService(this.bindableService)
                .build();
    }

    /**
     * Startup grpc {@link Server}
     */
    public void startup() {
        try {
            this.rpcServer.start();
            log.info("MessagePipe server bind port : {}, startup successfully.", this.configuration.getServerPort());
            this.rpcServer.awaitTermination();
        } catch (Exception e) {
            log.error("MessagePipe server startup failed.", e);
        }
    }

    /**
     * Shutdown grpc {@link Server}
     */
    public void shutdown() {
        try {
            log.info("MessagePipe server shutting down.");
            this.rpcServer.shutdown();
            long waitTime = 100;
            long timeConsuming = 0;
            while (!this.rpcServer.isShutdown()) {
                log.info("MessagePipe server stopping....，total time consuming：{}", timeConsuming);
                timeConsuming += waitTime;
                Thread.sleep(waitTime);
            }
            log.info("MessagePipe server stop successfully.");
        } catch (Exception e) {
            log.error("MessagePipe server shutdown failed.", e);
        }
    }

    @Override
    public void destroy() throws Exception {
        this.shutdown();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        this.buildServer();
        this.startup();
    }
}
