package org.minbox.framework.message.pipe.core.untis;

import org.minbox.framework.message.pipe.core.exception.MessagePipeException;

import java.net.InetAddress;

/**
 * @author 恒宇少年
 */
public class InternetAddressUtils {

    public static String getLocalHost() {
        try {
            InetAddress inetAddress = InetAddress.getLocalHost();
            return inetAddress.getHostAddress();
        } catch (Exception e) {
            throw new MessagePipeException(e.getMessage(), e);
        }
    }
}
