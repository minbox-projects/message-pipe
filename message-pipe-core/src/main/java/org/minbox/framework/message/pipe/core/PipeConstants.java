package org.minbox.framework.message.pipe.core;

/**
 * The constants
 *
 * @author 恒宇少年
 */
public interface PipeConstants {
    /**
     * The name of the service registered by the client to nacos
     */
    String CLIENT_SERVICE_NAME = "message-pipe-client-services";
    /**
     * Key stored in the metadata collection
     */
    String PIPE_NAMES_METADATA_KEY = "bindingPipeNames";
    /**
     * The pipeNames split
     */
    String PIPE_NAME_SPLIT = ",";
    /**
     * The key name pattern of pipe queue
     */
    String PIPE_NAME_PATTERN = "(.*?).queue";
}
