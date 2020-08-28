package org.minbox.framework.message.pipe.core.information;

import lombok.Getter;
import lombok.Setter;
import org.minbox.framework.message.pipe.core.ClientStatus;
import org.minbox.framework.message.pipe.core.PipeConstants;

/**
 * client information
 *
 * @author 恒宇少年
 */
@Getter
public class ClientInformation {
    /**
     * The client id string pattern
     */
    private static final String CLIENT_ID_PATTERN = "%s::%d";
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
    /**
     * this client binding pipe names
     */
    private String[] bindingPipeNames;

    public ClientInformation(String address, int port) {
        this.address = address;
        this.port = port;
    }

    public ClientInformation(String address, int port, String[] bindingPipeNames) {
        this.address = address;
        this.port = port;
        this.bindingPipeNames = bindingPipeNames;
    }

    /**
     * Get formatted clientId
     *
     * @return The current client id
     */
    public String getClientId() {
        return String.format(CLIENT_ID_PATTERN, this.address, this.port);
    }

    /**
     * Get new {@link ClientInformation} instance
     *
     * @param address client address
     * @param port    client port
     * @return {@link ClientInformation} instance
     */
    public static ClientInformation valueOf(String address, int port, String bindingPipeNames) {
        if (bindingPipeNames != null && bindingPipeNames.length() > 0) {
            return new ClientInformation(address, port, bindingPipeNames.split(PipeConstants.PIPE_NAME_SPLIT));
        }
        return new ClientInformation(address, port);
    }
}
