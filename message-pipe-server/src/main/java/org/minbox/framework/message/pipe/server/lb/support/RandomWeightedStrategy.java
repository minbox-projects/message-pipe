package org.minbox.framework.message.pipe.server.lb.support;

import org.minbox.framework.message.pipe.core.ClientInformation;
import org.minbox.framework.message.pipe.server.exception.MessagePipeException;
import org.minbox.framework.message.pipe.server.lb.ClientLoadBalanceStrategy;
import org.minbox.framework.message.pipe.server.lb.LoadBalanceNode;
import org.springframework.util.ObjectUtils;

import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * The {@link ClientLoadBalanceStrategy} random strategy
 *
 * @author 恒宇少年
 * @see DefaultLoadBalanceStrategy
 * @see ClientLoadBalanceStrategy
 */
public class RandomWeightedStrategy extends DefaultLoadBalanceStrategy {
    private TreeMap<Double, LoadBalanceNode> nodes = new TreeMap();

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
        if (ObjectUtils.isEmpty(clients)) {
            throw new MessagePipeException("Load balancing client list is empty.");
        }
        List<LoadBalanceNode> loadBalanceNodes = initNodeList(clients);
        loadBalanceNodes.stream().forEach(node -> {
            double lastWeight = this.nodes.size() == 0 ? 0 : this.nodes.lastKey().doubleValue();
            this.nodes.put(node.getInitWeight() + lastWeight, node);
        });
        Double randomWeight = this.nodes.lastKey() * Math.random();
        SortedMap<Double, LoadBalanceNode> tailMap = this.nodes.tailMap(randomWeight, false);
        if (ObjectUtils.isEmpty(tailMap)) {
            throw new MessagePipeException("No load balancing node was found");
        }
        return this.nodes.get(tailMap.firstKey()).getClient();
    }
}
