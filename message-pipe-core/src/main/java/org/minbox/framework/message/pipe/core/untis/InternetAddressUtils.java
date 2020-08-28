package org.minbox.framework.message.pipe.core.untis;

import java.net.*;
import java.util.Enumeration;

/**
 * The internet address utils
 *
 * @author 恒宇少年
 */
public class InternetAddressUtils {
    /**
     * Obtain the IPv4 address according to the network card
     *
     * @return IPv4 address
     */
    public static String getLocalIpByNetCard() {
        try {
            for (Enumeration<NetworkInterface> e = NetworkInterface.getNetworkInterfaces(); e.hasMoreElements(); ) {
                NetworkInterface item = e.nextElement();
                for (InterfaceAddress address : item.getInterfaceAddresses()) {
                    if (item.isLoopback() || !item.isUp()) {
                        continue;
                    }
                    if (address.getAddress() instanceof Inet4Address) {
                        Inet4Address inet4Address = (Inet4Address) address.getAddress();
                        return inet4Address.getHostAddress();
                    }
                }
            }
            return InetAddress.getLocalHost().getHostAddress();
        } catch (SocketException | UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Obtain Ipv4 address according to {@link InetAddress}
     *
     * @return Ipv4 address
     */
    public static String getLocalIP() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }
}
