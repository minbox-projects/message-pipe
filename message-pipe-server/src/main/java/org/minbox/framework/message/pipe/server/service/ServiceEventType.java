package org.minbox.framework.message.pipe.server.service;

/**
 * The {@link ServiceEvent} Type when triggered
 *
 * @author 恒宇少年
 */
public enum ServiceEventType {
    /**
     * Register a new client
     */
    REGISTER,
    /**
     * Heartbeat check
     */
    HEART_BEAT,
    /**
     * Reset local client list
     */
    RESET_INSTANCE,
    /**
     * client expired
     */
    EXPIRE
}
