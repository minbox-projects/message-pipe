package org.minbox.framework.message.pipe.client;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.minbox.framework.message.pipe.core.information.ServerInformation;
import org.springframework.util.ObjectUtils;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * The server manager
 *
 * @author 恒宇少年
 */
public class ServerManager {

    private static final String SERVER_ID_PATTERN = "%s::%d";
    /**
     * Server information map
     * <p>
     * Use serverId as the key of the collection
     */
    private static final ConcurrentMap<String, ServerInformation> SERVERS = new ConcurrentHashMap();
    /**
     * The channel corresponding to each server
     */
    private static final ConcurrentMap<String, ManagedChannel> SERVER_CHANNEL = new ConcurrentHashMap();

    /**
     * Get server id
     *
     * @param address The server address
     * @param port    The server port
     * @return serverId
     */
    public static String getServerId(String address, int port) {
        return String.format(SERVER_ID_PATTERN, address, port);
    }

    /**
     * Add to collection if it does not exist
     *
     * @param address server address
     * @param port    server port
     * @return serverId
     */
    public static String putIfNotPresent(String address, int port) {
        String serverId = getServerId(address, port);
        if (!SERVERS.containsKey(serverId)) {
            ServerInformation information = ServerInformation.valueOf(address, port);
            SERVERS.put(serverId, information);
        }
        return serverId;
    }

    /**
     * Establish a channel with the server
     *
     * @param serverId The serverId
     * @return {@link ManagedChannel} instance
     */
    public static synchronized ManagedChannel establishChannel(String serverId) {
        ServerInformation information = SERVERS.get(serverId);
        ManagedChannel channel = SERVER_CHANNEL.get(serverId);
        if (ObjectUtils.isEmpty(channel)) {
            channel = ManagedChannelBuilder.forAddress(information.getAddress(), information.getPort())
                    .usePlaintext()
                    .build();
            SERVER_CHANNEL.put(serverId, channel);
        }
        return channel;
    }

    /**
     * Delete unavailable server connection channel
     * <p>
     * If an unavailable Status appears during access after the connection is established,
     * delete the cached channel through this method
     *
     * @param serverId The serverId
     */
    public static void removeChannel(String serverId) {
        SERVER_CHANNEL.remove(serverId);
    }
}
