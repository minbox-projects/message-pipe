package org.minbox.framework.message.pipe;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import lombok.extern.slf4j.Slf4j;
import org.minbox.framework.message.pipe.exception.MessagePipeException;
import org.minbox.framework.message.pipe.manager.MessagePipeManager;

/**
 * The {@link MessagePipe} service container
 * <p>
 * the service container listener
 * loads the message pipeline data of the specified name sequentially from the redis queue
 *
 * @author 恒宇少年
 */
@Slf4j
public class MessagePipeServerContainer {
    private static final int DEFAULT_SERVER_PORT = 5200;
    /**
     * The server port
     */
    private int port;
    /**
     * The grpc server instance
     */
    private Server rpcServer;
    /**
     * The {@link MessagePipe} manager
     *
     * @see org.minbox.framework.message.pipe.manager.AbstractMessagePipeManager
     * @see org.minbox.framework.message.pipe.manager.DefaultMessagePipeManager
     */
    private MessagePipeManager manager;
    /**
     * Bound service interface instance
     *
     * @see MessageService
     */
    private BindableService bindableService;

    /**
     * Instantiate using the default port {@link #DEFAULT_SERVER_PORT}
     */
    public MessagePipeServerContainer(MessagePipeManager manager) {
        this(DEFAULT_SERVER_PORT, manager);
    }

    /**
     * Instantiate with a custom port number
     * <p>
     * Instantiate grpc {@link Server} object through the {@link #buildServer()} method
     *
     * @param port Server port
     */
    public MessagePipeServerContainer(int port, MessagePipeManager manager) {
        this.manager = manager;
        this.port = port;
        if (this.port <= 0 || this.port > 65535) {
            throw new MessagePipeException("MessageServer port must be greater than 0 and less than 65535");
        }
        if (this.manager == null) {
            throw new MessagePipeException("MessagePipeManager cannot be null.");
        }
        this.bindableService = new MessageService(this.manager);
        this.buildServer();
    }

    /**
     * Build the grpc {@link Server} instance
     */
    private void buildServer() {
        this.rpcServer = ServerBuilder
                .forPort(this.port)
                .addService(this.bindableService)
                .build();
    }

    /**
     * Startup grpc {@link Server}
     */
    public void startup() {
        try {
            log.info("MessageServer bind port : {}, starting...", this.port);
            this.rpcServer.start();
            log.info("MessageServer startup successfully.");
            this.rpcServer.awaitTermination();
        } catch (Exception e) {
            log.error("MessageServer startup failed.", e);
        }
    }

    /**
     * Shutdown grpc {@link Server}
     */
    public void shutdown() {
        try {
            this.rpcServer.shutdown();
            long waitTime = 100;
            long timeConsuming = 0;
            while (!this.rpcServer.isShutdown()) {
                log.info("MessageServer stopping....，total time consuming：{}", timeConsuming);
                timeConsuming += waitTime;
                Thread.sleep(waitTime);
            }
            log.info("MessageServer stop successfully.");
        } catch (Exception e) {
            log.error("MessageServer shutdown failed.", e);
        }

    }
}
