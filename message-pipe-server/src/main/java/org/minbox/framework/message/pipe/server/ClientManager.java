package org.minbox.framework.message.pipe.server;

import org.minbox.framework.message.pipe.core.ClientInformation;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
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
     * List of clients bound to the message pipeline
     * <p>
     * The key of this set is the name of the message channel bound to the client
     */
    private static final ConcurrentMap<String, List<String>> PIPE_CLIENTS = new ConcurrentHashMap();

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
        List<String> pipeClients = Optional.ofNullable(PIPE_CLIENTS.get(pipeName)).orElse(new ArrayList());
        pipeClients.add(clientId);
        PIPE_CLIENTS.put(pipeName, pipeClients);
    }
}
