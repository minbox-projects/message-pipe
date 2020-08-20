package org.minbox.framework.message.pipe.client.process;

/**
 * Message Processor function
 *
 * @author 恒宇少年
 */
@FunctionalInterface
public interface MessageProcessor {
    /**
     * Execute processing message
     *
     * @param requestId   The message request id
     * @param pipeName    The pipe to which the message belongs
     * @param messageBody The message byte body
     * @return Return "true" after successful execution
     */
    boolean processing(String requestId, String pipeName, byte[] messageBody);
}
