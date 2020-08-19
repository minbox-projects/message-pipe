package org.minbox.framework.message.pipe.server;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.minbox.framework.message.pipe.core.ClientInformation;
import org.springframework.util.ObjectUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * client collection
 * <p>
 * Provides operating methods for client sets stored in memory
 *
 * @author 恒宇少年
 */
public class ClientManager {

    private static final String CLIENT_ID_PATTERN = "%s::%d";
    /**
     * There is a list of all clients
     */
    private static final ConcurrentMap<String, ClientInformation> CLIENTS = new ConcurrentHashMap();
    /**
     * Channel established with client
     * <p>
     * This channel is used to send messages to the specified client
     */
    private static final ConcurrentMap<String, ManagedChannel> CLIENT_CHANNELS = new ConcurrentHashMap();
    /**
     * List of clients bound to the message pipeline
     * <p>
     * The key of this set is the name of the message channel bound to the client
     */
    private static final ConcurrentMap<String, Set<String>> PIPE_CLIENTS = new ConcurrentHashMap();

    /**
     * If it does not exist, add it to the client collection
     *
     * @param address client address
     * @param port    client port
     * @return the client id
     */
    public static String putIfNotPresent(String address, int port) {
        String clientId = getClientId(address, port);
        ClientInformation clientInformation = ClientInformation.valueOf(address, port);
        if (!CLIENTS.containsKey(clientId)) {
            CLIENTS.put(clientId, clientInformation);
        }
        return clientId;
    }

    /**
     * get formatted clientId
     *
     * @param address the client address
     * @param port    the client port
     * @return clientId
     */
    public static String getClientId(String address, int port) {
        return String.format(CLIENT_ID_PATTERN, address, port);
    }

    /**
     * Check if the client already exists
     *
     * @param clientId client id
     * @return Return "true" means it already exists
     */
    public static boolean containsClient(String clientId) {
        return CLIENTS.containsKey(clientId);
    }

    /**
     * Get client information from {@link #CLIENTS}
     *
     * @param clientId client id
     * @return The client information {@link ClientInformation}
     */
    public static ClientInformation getClient(String clientId) {
        return CLIENTS.get(clientId);
    }

    /**
     * Get all client from {@link #CLIENTS}
     *
     * @return all clients {@link ClientInformation}
     */
    public static List<ClientInformation> getAllClient() {
        return CLIENTS.values().stream().collect(Collectors.toList());
    }

    /**
     * Update client information
     *
     * @param information The {@link ClientInformation} instance
     */
    public static void updateClientInformation(ClientInformation information) {
        String clientId = getClientId(information.getAddress(), information.getPort());
        CLIENTS.put(clientId, information);
    }

    /**
     * Bind client to message pipe {@link #PIPE_CLIENTS}
     *
     * @param pipeName message pipe name
     * @param clientId client id
     */
    public static void bindClientToPipe(String pipeName, String clientId) {
        Set<String> pipeClients = Optional.ofNullable(PIPE_CLIENTS.get(pipeName)).orElse(new HashSet<>());
        pipeClients.add(clientId);
        PIPE_CLIENTS.put(pipeName, pipeClients);
    }

    /**
     * Get message pipe bind clients information {@link ClientInformation}
     *
     * @param pipeName message pipe name
     * @return The message pipe bind clients
     */
    public static List<ClientInformation> getPipeBindClients(String pipeName) {
        List<ClientInformation> clientInformationList = new ArrayList<>();
        Set<String> clientIds = PIPE_CLIENTS.get(pipeName);
        if (!ObjectUtils.isEmpty(clientIds)) {
            clientIds.stream().forEach(clientId -> clientInformationList.add(CLIENTS.get(clientId)));
        }
        return clientInformationList;
    }

    /**
     * Establish a channel for distributing messages with the client
     *
     * @param clientInformation The client {@link ClientInformation}
     */
    public static ManagedChannel establishClientChannel(ClientInformation clientInformation) {
        String clientId = getClientId(clientInformation.getAddress(), clientInformation.getPort());
        ManagedChannel channel = ManagedChannelBuilder.forAddress(clientInformation.getAddress(), clientInformation.getPort())
                .usePlaintext()
                .build();
        if (!CLIENT_CHANNELS.containsKey(clientId)) {
            CLIENT_CHANNELS.put(clientId, channel);
        }
        return channel;
    }
}
