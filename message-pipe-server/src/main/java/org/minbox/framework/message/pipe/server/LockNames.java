package org.minbox.framework.message.pipe.server;

/**
 * Define the LockName when processing the message
 *
 * @author 恒宇少年
 */
public enum LockNames {
    MESSAGE_QUEUE("%s.queue.lock"),
    TAKE_MESSAGE("%s.take.lock"),
    PUT_MESSAGE("%s.put.lock");

    LockNames(String pattern) {
        this.pattern = pattern;
    }

    private String pattern;

    /**
     * Format lockName according with {@link #pattern}
     *
     * @param lockName The {@link MessagePipe} name
     * @return Formatted lockName，example："test.queue.lock"
     */
    public String format(String lockName) {
        return String.format(this.pattern, lockName);
    }
}
