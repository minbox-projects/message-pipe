package org.minbox.framework.message.pipe.server.service.discovery;

import lombok.extern.slf4j.Slf4j;
import org.minbox.framework.message.pipe.core.ClientStatus;
import org.minbox.framework.message.pipe.core.exception.MessagePipeException;
import org.minbox.framework.message.pipe.core.information.ClientInformation;
import org.minbox.framework.message.pipe.server.MessagePipe;
import org.minbox.framework.message.pipe.server.config.MessagePipeConfiguration;
import org.minbox.framework.message.pipe.server.config.ServerConfiguration;
import org.minbox.framework.message.pipe.server.service.ServiceEvent;
import org.minbox.framework.message.pipe.server.service.ServiceEventType;
import org.springframework.context.ApplicationListener;
import org.springframework.util.ObjectUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Pattern;

/**
 * The client service discovery
 * <p>
 * Provide client service list query, update and other processing
 *
 * @author 恒宇少年
 */
@Slf4j
public class ClientServiceDiscovery implements ServiceDiscovery, ApplicationListener<ServiceEvent> {
    /**
     * The bean name of {@link ClientServiceDiscovery}
     */
    public static final String BEAN_NAME = "clientServiceDiscovery";
    /**
     * There is a list of all clients
     */
    private static final ConcurrentMap<String, ClientInformation> CLIENTS = new ConcurrentHashMap();
    /**
     * List of clients bound to the message pipeline
     * <p>
     * The key of this set is the name of the message channel bound to the client
     */
    private static final ConcurrentMap<String, Set<String>> PIPE_CLIENTS = new ConcurrentHashMap();
    private MessagePipeConfiguration configuration;
    private ServerConfiguration serverConfiguration;

    public ClientServiceDiscovery(MessagePipeConfiguration configuration, ServerConfiguration serverConfiguration) {
        this.configuration = configuration;
        this.serverConfiguration = serverConfiguration;
    }

    /**
     * Obtain a healthy load-balanced client instance
     *
     * @param pipeNamePattern The {@link org.minbox.framework.message.pipe.server.MessagePipe} pattern name
     * @return
     * @throws MessagePipeException
     */
    @Override
    public ClientInformation lookup(String pipeNamePattern) throws MessagePipeException {
        List<ClientInformation> clients = new ArrayList<>();
        Set<String> clientIds = regexGetClientIds(pipeNamePattern);
        if (!ObjectUtils.isEmpty(clientIds)) {
            clientIds.stream().forEach(clientId -> {
                ClientInformation client = CLIENTS.get(clientId);
                if (ClientStatus.ON_LINE == client.getStatus()) {
                    clients.add(client);
                }
            });
        }
        if (!ObjectUtils.isEmpty(clients)) {
            return configuration.getLoadBalanceStrategy().lookup(clients);
        }
        return null;
    }

    /**
     * Use regular expressions to obtain ClientIds
     *
     * @param pipeName The {@link MessagePipe} specific name
     * @return The {@link MessagePipe} binding clientIds
     */
    protected Set<String> regexGetClientIds(String pipeName) {
        Iterator<String> iterator = PIPE_CLIENTS.keySet().iterator();
        while (iterator.hasNext()) {
            // PipeName when the client is registered，May be a regular expression
            String pipeNamePattern = iterator.next();
            boolean isMatch = Pattern.compile(pipeNamePattern).matcher(pipeName).matches();
            if (isMatch) {
                return PIPE_CLIENTS.get(pipeNamePattern);
            }
        }
        return null;
    }

    /**
     * Listen for {@link ServiceEvent}
     * <p>
     * Process client services according to different event types
     *
     * @param event The {@link ServiceEvent} instance
     */
    @Override
    public void onApplicationEvent(ServiceEvent event) {
        ServiceEventType eventType = event.getServiceEventType();
        List<ClientInformation> clients = event.getClients();
        switch (eventType) {
            case REGISTER:
                this.handingRegister(clients);
                break;
            case HEART_BEAT:
                this.handingHeartBeat(clients);
                break;
            case RESET_INSTANCE:
                this.handingResetInstances(clients);
                break;
            case EXPIRE:
                this.handingExpired();
                break;
        }
    }

    /**
     * Register a service
     * <p>
     * Cache client information in a local collection
     * The relationship between the binding pipeline and the client
     *
     * @param information The client information
     */
    protected void registerService(ClientInformation information) {
        information.setStatus(ClientStatus.ON_LINE);
        this.CLIENTS.put(information.getClientId(), information);
        String[] bindingPipeNames = information.getBindingPipeNames();
        if (!ObjectUtils.isEmpty(bindingPipeNames)) {
            for (String pipeName : bindingPipeNames) {
                Set<String> pipeBindClientIds = PIPE_CLIENTS.get(pipeName);
                pipeBindClientIds = Optional.ofNullable(pipeBindClientIds).orElse(new HashSet<>());
                pipeBindClientIds.add(information.getClientId());
                PIPE_CLIENTS.put(pipeName, pipeBindClientIds);
                log.info("Client, Pipe: {}, IP: {}, Port: {}, registration is successful.",
                        pipeName, information.getAddress(), information.getPort());
            }
        }
    }

    /**
     * Handling reset client instance collection
     *
     * @param clients Client list after reset
     * @see ServiceEventType#RESET_INSTANCE
     */
    protected void handingResetInstances(List<ClientInformation> clients) {
        this.CLIENTS.clear();
        this.PIPE_CLIENTS.clear();
        clients.stream().forEach(client -> this.registerService(client));
        log.info("Client collection, reset instance list is complete.");
    }

    /**
     * Dealing with client expiration
     */
    protected void handingExpired() {
        if (!ObjectUtils.isEmpty(CLIENTS)) {
            Long currentTime = System.currentTimeMillis();
            CLIENTS.values().stream().forEach(client -> {
                String clientId = client.getClientId();
                long intervalSeconds = (currentTime - client.getLastReportTime()) / 1000;
                if (intervalSeconds > serverConfiguration.getExpiredExcludeThresholdSeconds()
                        && ClientStatus.ON_LINE.equals(client.getStatus())) {
                    client.setStatus(ClientStatus.OFF_LINE);
                    log.info("MessagePipe Client：{}，status updated to offline.", clientId);
                } else if (intervalSeconds <= serverConfiguration.getExpiredExcludeThresholdSeconds()
                        && ClientStatus.OFF_LINE.equals(client.getStatus())) {
                    client.setStatus(ClientStatus.ON_LINE);
                    log.info("MessagePipe Client：{}，status updated to online.", clientId);
                }
            });
        }
    }

    /**
     * List of registered clients
     *
     * @param clients List of clients waiting to be registered
     */
    protected void handingRegister(List<ClientInformation> clients) {
        clients.stream().forEach(client -> this.registerService(client));
    }

    /**
     * Update the last heartbeat time of the client
     * <p>
     * When receiving the heartbeat, if the client is not registered, perform registration
     *
     * @param clients List of clients waiting to update their heartbeat time
     */
    protected void handingHeartBeat(List<ClientInformation> clients) {
        Long currentTime = System.currentTimeMillis();
        clients.stream().forEach(client -> {
            log.debug("Receiving client: {}, heartbeat sent.", client.getClientId());
            ClientInformation cacheClient = CLIENTS.get(client.getClientId());
            if (ObjectUtils.isEmpty(cacheClient)) {
                client.setLastReportTime(currentTime);
                this.registerService(client);
            } else {
                cacheClient.setLastReportTime(currentTime);
            }
        });
    }
}
