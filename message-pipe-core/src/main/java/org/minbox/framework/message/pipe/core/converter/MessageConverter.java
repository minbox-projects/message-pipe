package org.minbox.framework.message.pipe.core.converter;

import org.minbox.framework.message.pipe.core.Message;

/**
 * The {@link Message} converter function
 *
 * @author 恒宇少年
 */
@FunctionalInterface
public interface MessageConverter {
    /**
     * Convert {@link Message} to return value type instance
     *
     * @param message The {@link Message} instance
     * @param <R>     The type want to convert
     * @return Converted instance
     */
    <R> R convert(Message message);
}
