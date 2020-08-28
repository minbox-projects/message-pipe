package org.minbox.framework.message.pipe.server.distribution;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.minbox.framework.message.pipe.core.exception.MessagePipeException;
import org.minbox.framework.message.pipe.core.information.ClientInformation;
import org.springframework.util.ObjectUtils;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author 恒宇少年
 */
public class ClientChannelManager {
    /**
     * Store the connection channel of each client
     * <p>
     * The key is {@link ClientInformation#getClientId()}
     */
    private static final ConcurrentMap<String, ManagedChannel> CLIENT_CHANNEL = new ConcurrentHashMap();

    /**
     * Establish a client channel
     *
     * @param information The {@link ClientInformation} instance
     * @return {@link ManagedChannel} instance
     */
    public static ManagedChannel establishChannel(ClientInformation information) {
        String clientId = information.getClientId();
        if (ObjectUtils.isEmpty(information)) {
            throw new MessagePipeException("Client: " + clientId + " is not registered");
        }
        ManagedChannel channel = CLIENT_CHANNEL.get(clientId);
        if (ObjectUtils.isEmpty(channel) || channel.isShutdown()) {
            channel = ManagedChannelBuilder.forAddress(information.getAddress(), information.getPort())
                    .usePlaintext()
                    .build();
            CLIENT_CHANNEL.put(clientId, channel);
        }
        return channel;
    }

    /**
     * Remove client {@link ManagedChannel}
     *
     * @param clientId The client id
     */
    public static void removeChannel(String clientId) {
        CLIENT_CHANNEL.remove(clientId);
    }
}
