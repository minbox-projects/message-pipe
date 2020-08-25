package org.minbox.framework.message.pipe.client.process;

/**
 * Message Processor function
 *
 * @author 恒宇少年
 */
public interface MessageProcessor {
    /**
     * binding pipe name
     * <p>
     * Only execute the {@link #processing} method when the pipe name matches the return value
     *
     * @return The {@link MessageProcessor} binding pipe name
     */
    String bindingPipeName();

    /**
     * Execute processing message
     *
     * @param specificPipeName The specific pipe name, if the regular expression matches,
     *                         this parameter is the target pipe name
     * @param requestId        The message request id
     * @param messageBody      The message byte body
     * @return Return "true" after successful execution
     */
    boolean processing(String specificPipeName, String requestId, byte[] messageBody);
}
