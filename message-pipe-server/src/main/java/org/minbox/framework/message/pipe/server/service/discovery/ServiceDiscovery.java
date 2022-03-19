package org.minbox.framework.message.pipe.server.service.discovery;

import org.minbox.framework.message.pipe.core.exception.MessagePipeException;
import org.minbox.framework.message.pipe.core.information.ClientInformation;
import org.minbox.framework.message.pipe.server.MessagePipe;

/**
 * The service discovery function
 *
 * @author 恒宇少年
 */
public interface ServiceDiscovery {
    /**
     * Obtain a bound client instance based on the name of the message pipe
     * <p>
     * The obtained instance supports load balancing
     *
     * @param pipeNamePattern The {@link MessagePipe} pattern name
     * @return The {@link ClientInformation} instance
     * @throws MessagePipeException The {@link MessagePipeException} instance
     */
    ClientInformation lookup(String pipeNamePattern) throws MessagePipeException;

    /**
     * Check for healthy clients
     *
     * @param pipeNamePattern The {@link MessagePipe} pattern name
     * @return Returns true if exists
     * @throws MessagePipeException The {@link MessagePipeException} instance
     */
    boolean checkHaveHealthClient(String pipeNamePattern) throws MessagePipeException;
}
