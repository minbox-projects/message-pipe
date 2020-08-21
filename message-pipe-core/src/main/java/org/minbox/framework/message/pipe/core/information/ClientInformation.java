package org.minbox.framework.message.pipe.core.information;

import lombok.Getter;
import lombok.Setter;
import org.minbox.framework.message.pipe.core.ClientStatus;

/**
 * client information
 *
 * @author 恒宇少年
 */
@Getter
public class ClientInformation {
    /**
     * client address
     */
    private String address;
    /**
     * client port
     */
    private int port;
    /**
     * first online time
     */
    @Setter
    private long onlineTime;
    /**
     * last report time
     */
    @Setter
    private long lastReportTime;
    /**
     * this client status
     */
    @Setter
    private ClientStatus status;

    public ClientInformation(String address, int port) {
        this.address = address;
        this.port = port;
    }

    /**
     * Get new {@link ClientInformation} instance
     *
     * @param address client address
     * @param port    client port
     * @return {@link ClientInformation} instance
     */
    public static ClientInformation valueOf(String address, int port) {
        return new ClientInformation(address, port);
    }
}
