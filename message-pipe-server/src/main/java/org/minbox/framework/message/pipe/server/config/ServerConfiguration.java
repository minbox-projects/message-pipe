package org.minbox.framework.message.pipe.server.config;

import lombok.Data;
import lombok.experimental.Accessors;
import org.minbox.framework.message.pipe.server.service.GRpcServerApplicationService;

/**
 * server configuration
 *
 * @author 恒宇少年
 */
@Data
@Accessors(chain = true)
public class ServerConfiguration {
    /**
     * The server port
     *
     * @see GRpcServerApplicationService
     */
    private int serverPort = 5200;
    /**
     * client expired executor pool size
     */
    private int expiredPoolSize = 5;
    /**
     * Time threshold for excluding clients
     */
    private long expiredExcludeThresholdSeconds = 30;
    /**
     * Check the client timeout interval in seconds
     */
    private long checkClientExpiredIntervalSeconds = 10;
    /**
     * Maximum number of message pipes
     */
    private int maxMessagePipeCount = 100;
}
