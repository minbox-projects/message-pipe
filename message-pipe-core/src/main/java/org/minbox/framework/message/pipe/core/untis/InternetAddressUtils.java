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
        return getLocalIpByNetCard(null);
    }

    /**
     * Obtain the IPv4 address according to the network card
     *
     * @param specifiedInterface The specified network interface name, e.g., "eth0", "en0"
     * @return IPv4 address
     */
    public static String getLocalIpByNetCard(String specifiedInterface) {
        try {
            String candidateAddress = null;
            for (Enumeration<NetworkInterface> e = NetworkInterface.getNetworkInterfaces(); e.hasMoreElements(); ) {
                NetworkInterface item = e.nextElement();
                for (InterfaceAddress address : item.getInterfaceAddresses()) {
                    if (item.isLoopback() || !item.isUp()) {
                        continue;
                    }
                    if (address.getAddress() instanceof Inet4Address) {
                        Inet4Address inet4Address = (Inet4Address) address.getAddress();
                        String ip = inet4Address.getHostAddress();

                        // If a specific interface is specified, strictly match it
                        if (specifiedInterface != null && !specifiedInterface.isEmpty()) {
                            if (item.getName().equals(specifiedInterface)) {
                                return ip;
                            }
                            continue;
                        }

                        if (isPreferredInterface(item.getName())) {
                            return ip;
                        }
                        if (isIgnoredInterface(item.getName())) {
                            continue;
                        }
                        if (candidateAddress == null) {
                            candidateAddress = ip;
                        }
                    }
                }
            }
            if (candidateAddress != null) {
                return candidateAddress;
            }
            return InetAddress.getLocalHost().getHostAddress();
        } catch (SocketException | UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean isPreferredInterface(String interfaceName) {
        return interfaceName.startsWith("eth")
                || interfaceName.startsWith("en")
                || interfaceName.startsWith("wlan")
                || interfaceName.startsWith("em")
                || interfaceName.startsWith("p");
    }

    private static boolean isIgnoredInterface(String interfaceName) {
        return interfaceName.startsWith("bridge")
                || interfaceName.startsWith("docker")
                || interfaceName.startsWith("veth")
                || interfaceName.startsWith("utun")
                || interfaceName.startsWith("vmenet")
                || interfaceName.startsWith("awdl")
                || interfaceName.startsWith("llw");
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
