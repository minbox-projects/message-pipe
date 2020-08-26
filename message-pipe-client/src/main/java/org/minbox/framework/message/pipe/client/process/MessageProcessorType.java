package org.minbox.framework.message.pipe.client.process;

/**
 * The {@link MessageProcessor} type define
 *
 * @author 恒宇少年
 */
public enum MessageProcessorType {
    /**
     * The specific type
     * <p>
     * a message processor corresponds to a message pipeline
     */
    SPECIFIC,
    /**
     * The regex type
     * <p>
     * one processor corresponds to multiple expression pipelines
     */
    REGEX
}
