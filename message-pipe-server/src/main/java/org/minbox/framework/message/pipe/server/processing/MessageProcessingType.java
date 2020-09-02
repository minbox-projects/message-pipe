package org.minbox.framework.message.pipe.server.processing;

/**
 * Message processing type
 *
 * @author 恒宇少年
 */
public enum MessageProcessingType {
    /**
     * Push a message to pipeline
     * <p>
     * Wake up MessageDistributionExecutor
     * perform a message distribution of the specified channel
     */
    PUSH
}
