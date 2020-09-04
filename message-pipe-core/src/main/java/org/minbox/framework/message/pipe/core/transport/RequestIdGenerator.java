package org.minbox.framework.message.pipe.core.transport;

/**
 * The requestId generator function
 *
 * @author 恒宇少年
 */
@FunctionalInterface
public interface RequestIdGenerator {
    /**
     * Generate a new requestId
     *
     * @return The requestId value
     */
    String generate();
}
