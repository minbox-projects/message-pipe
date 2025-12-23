package org.minbox.framework.message.pipe.server.lb.support;

import org.minbox.framework.message.pipe.core.information.ClientInformation;
import org.minbox.framework.message.pipe.core.exception.MessagePipeException;
import org.minbox.framework.message.pipe.server.lb.ClientLoadBalanceStrategy;
import org.minbox.framework.message.pipe.server.lb.LoadBalanceNode;
import org.springframework.util.ObjectUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * The {@link ClientLoadBalanceStrategy} random strategy
 *
 * @author 恒宇少年
 * @see ClientLoadBalanceStrategy
 */
public class RandomWeightedStrategy implements ClientLoadBalanceStrategy {
    /**
     * Default constructor for RandomWeightedStrategy
     */
    public RandomWeightedStrategy() {
    }

    /**
     * lookup client load-balanced address {@link LoadBalanceNode#getClient()}
     * Lookup according to random weight admin address
     * get firstKey by {@link SortedMap#tailMap(Object)}
     *
     * @param clients message pipe bind clients
     * @return Load-balanced {@link ClientInformation}
     * @throws MessagePipeException message pipe exception
     */
    @Override
    public ClientInformation lookup(List<ClientInformation> clients) throws MessagePipeException {
        TreeMap<Double, LoadBalanceNode> nodes = new TreeMap();
        List<LoadBalanceNode> loadBalanceNodes =
                clients.stream().map(client -> new LoadBalanceNode(client)).collect(Collectors.toList());
        loadBalanceNodes.stream().forEach(node -> {
            double lastWeight = nodes.size() == 0 ? 0 : nodes.lastKey().doubleValue();
            nodes.put(node.getInitWeight() + lastWeight, node);
        });
        Double randomWeight = nodes.lastKey() * Math.random();
        SortedMap<Double, LoadBalanceNode> tailMap = nodes.tailMap(randomWeight, false);
        if (ObjectUtils.isEmpty(tailMap)) {
            throw new MessagePipeException("No load balancing node was found");
        }
        return nodes.get(tailMap.firstKey()).getClient();
    }
}
