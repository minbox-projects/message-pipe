package org.minbox.framework.message.pipe.client.registrar;

/**
 * The client registrar service
 *
 * @author 恒宇少年
 */
public interface RegistrarService {
    /**
     * Register client to server
     *
     * @param serverAddress The server address
     * @param serverPort    The server port
     */
    void register(String serverAddress, int serverPort);
}
