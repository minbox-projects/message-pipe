package org.minbox.framework.message.pipe.core.information;

import lombok.Getter;

/**
 * The server information
 *
 * @author 恒宇少年
 */
@Getter
public class ServerInformation {
    /**
     * Server address
     */
    private String address;
    /**
     * Server port
     */
    private int port;

    private ServerInformation(String address, int port) {
        this.address = address;
        this.port = port;
    }

    /**
     * Get {@link ServerInformation} instance
     *
     * @param address server address
     * @param port    server port
     * @return The {@link ServerInformation} instance
     */
    public static ServerInformation valueOf(String address, int port) {
        return new ServerInformation(address, port);
    }
}
