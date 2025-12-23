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
     * Default constructor for ServerConfiguration
     */
    public ServerConfiguration() {
    }
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
    private long checkClientExpiredIntervalSeconds = 5;
    /**
     * Maximum number of message pipes
     */
    private int maxMessagePipeCount = 100;
    /**
     * Interval for cleaning up expired message pipe threads, in seconds
     */
    private long cleanupExpiredMessagePipeIntervalSeconds = 10;
    /**
     * The threshold for determining an expired message pipe, in seconds
     */
    private long cleanupExpiredMessagePipeThresholdSeconds = 1800;
    /**
     * Configure the message pipe name to exclude distribution
     * <p>
     * configure regular expression content
     */
    private String[] excludePipeNamePatterns;
}
