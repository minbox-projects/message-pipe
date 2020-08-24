package org.minbox.framework.message.pipe.server;

import lombok.extern.slf4j.Slf4j;
import org.minbox.framework.message.pipe.core.ClientStatus;
import org.minbox.framework.message.pipe.core.information.ClientInformation;
import org.minbox.framework.message.pipe.server.config.ServerConfiguration;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Client expiration processing executor
 * <p>
 * According to the threshold judgment,
 * if the last reported time and the current time exceed the threshold time (unit: second),
 * modify the client status to {@link ClientStatus#OFF_LINE}
 *
 * @author 恒宇少年
 */
@Slf4j
public class ClientExpiredExecutor implements InitializingBean, DisposableBean {
    /**
     * The bean name of {@link ClientExpiredExecutor}
     */
    public static final String BEAN_NAME = "clientExpiredExecutor";
    private ScheduledExecutorService expiredExecutorService;
    private ServerConfiguration configuration;

    /**
     * Initialize {@link ClientExpiredExecutor} according to the provided configuration parameters
     *
     * @param configuration The {@link ServerConfiguration}
     */
    public ClientExpiredExecutor(ServerConfiguration configuration) {
        this.configuration = configuration;
    }

    /**
     * Start eliminate expired client
     * <p>
     * If the client's last heartbeat time is greater than the timeout threshold,
     * the update status is performed
     */
    private void startEliminateExpiredClient() {
        this.expiredExecutorService.scheduleAtFixedRate(() -> {
            long currentTime = System.currentTimeMillis();
            List<ClientInformation> clients = ClientManager.getAllClient();
            if (clients != null && clients.size() > 0) {
                clients.stream().forEach(client -> {
                    String clientId = ClientManager.getClientId(client.getAddress(), client.getPort());
                    long intervalSeconds = (currentTime - client.getLastReportTime()) / 1000;
                    if (intervalSeconds > configuration.getExpiredExcludeThresholdSeconds()
                            && ClientStatus.ON_LINE.equals(client.getStatus())) {
                        client.setStatus(ClientStatus.OFF_LINE);
                        ClientManager.updateClientInformation(client);
                        log.info("MessagePipe Client：{}，status updated to offline.", clientId);
                    } else if (intervalSeconds <= configuration.getExpiredExcludeThresholdSeconds()
                            && ClientStatus.OFF_LINE.equals(client.getStatus())) {
                        client.setStatus(ClientStatus.ON_LINE);
                        ClientManager.updateClientInformation(client);
                        log.info("MessagePipe Client：{}，status updated to online.", clientId);
                    }
                });
            }
        }, 5, configuration.getCheckClientExpiredIntervalSeconds(), TimeUnit.SECONDS);
    }

    @Override
    public void destroy() throws Exception {
        log.info("MessagePipe ClientExpiredExecutor shutting down.");
        this.expiredExecutorService.shutdown();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        this.expiredExecutorService = Executors.newScheduledThreadPool(configuration.getExpiredPoolSize());
        this.startEliminateExpiredClient();
        log.info("MessagePipe ClientExpiredExecutor successfully started.");
    }
}
