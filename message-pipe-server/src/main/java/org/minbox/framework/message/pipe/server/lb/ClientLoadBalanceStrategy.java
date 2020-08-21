package org.minbox.framework.message.pipe.server.lb;

import org.minbox.framework.message.pipe.core.information.ClientInformation;
import org.minbox.framework.message.pipe.core.exception.MessagePipeException;

import java.util.List;

/**
 * Get client list load balancing interface definition
 *
 * @author 恒宇少年
 */
public interface ClientLoadBalanceStrategy {
    /**
     * Lookup a {@link ClientInformation}
     *
     * @param clients message pipe {@link ClientInformation} list
     * @return load-balanced client
     * @throws MessagePipeException message pipe exception
     */
    ClientInformation lookup(List<ClientInformation> clients) throws MessagePipeException;
}
