package org.minbox.framework.message.pipe.server.config;

import lombok.Data;
import lombok.experimental.Accessors;
import org.minbox.framework.message.pipe.server.distribution.MessageDistributionExecutor;
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
     * The number of threads for message distribution executor thread pool initialization
     */
    private int messageDistributionExecutorPoolSize = 50;
    /**
     * Message pipeline monitoring interval time, unit: second
     */
    private int monitorCheckIntervalSeconds = 5;
    /**
     * notify {@link MessageDistributionExecutor} interval
     */
    private long notifyIntervalMillSeconds = 10000;
}
