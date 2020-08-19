package org.minbox.framework.message.pipe.server.config;

import lombok.Data;
import lombok.experimental.Accessors;

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
     * @see org.minbox.framework.message.pipe.server.MessagePipeServerApplication
     */
    private int serverPort = 5200;
    /**
     * client expired executor pool size
     *
     * @see org.minbox.framework.message.pipe.server.ClientExpiredExecutor
     */
    private int expiredPoolSize = 5;
    /**
     * Time threshold for excluding clients
     *
     * @see org.minbox.framework.message.pipe.server.ClientExpiredExecutor
     */
    private long expiredExcludeThresholdSeconds = 30;
}
