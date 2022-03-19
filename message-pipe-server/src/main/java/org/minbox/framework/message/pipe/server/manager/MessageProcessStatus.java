package org.minbox.framework.message.pipe.server.manager;

/**
 * The message send status
 *
 * @author 恒宇少年
 */
public enum MessageProcessStatus {
    /**
     * Write message to queue exception
     */
    PUT_EXCEPTION,
    /**
     * The message send success
     */
    SEND_SUCCESS,
    /**
     * don't have health target client
     */
    NO_HEALTH_CLIENT,
    /**
     * Encountered once while sending message
     */
    SEND_EXCEPTION
}
