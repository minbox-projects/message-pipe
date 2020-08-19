package org.minbox.framework.message.pipe.server.lb.support;

import org.minbox.framework.message.pipe.core.ClientInformation;
import org.minbox.framework.message.pipe.server.exception.MessagePipeException;
import org.minbox.framework.message.pipe.server.lb.ClientLoadBalanceStrategy;
import org.minbox.framework.message.pipe.server.lb.LoadBalanceNode;
import org.springframework.util.ObjectUtils;

import java.util.LinkedList;
import java.util.List;

/**
 * The {@link ClientLoadBalanceStrategy} default support
 *
 * @author 恒宇少年
 * @see LoadBalanceNode
 */
public abstract class DefaultLoadBalanceStrategy implements ClientLoadBalanceStrategy {

    /**
     * Initialize the list of transformation nodes
     * Convert client address to {@link LoadBalanceNode}
     *
     * @param clients The message pipe {@link ClientInformation} list
     * @return {@link LoadBalanceNode} collection
     */
    protected List<LoadBalanceNode> initNodeList(List<ClientInformation> clients) {
        List<LoadBalanceNode> nodes = new LinkedList();
        clients.stream().forEach(client -> nodes.add(new LoadBalanceNode(client)));
        return nodes;
    }
}
