package org.minbox.framework.message.pipe.server.lb;

import org.minbox.framework.message.pipe.core.ClientInformation;

/**
 * Load Balance Node
 *
 * @author 恒宇少年
 */
public class LoadBalanceNode {
    /**
     * node init weight
     */
    private int initWeight = 1;
    /**
     * logging admin address
     */
    private ClientInformation client;
    /**
     * current weight
     */
    private int currentWeight;

    public LoadBalanceNode(ClientInformation client) {
        this.client = client;
    }

    public int getInitWeight() {
        return initWeight;
    }

    public void setInitWeight(int initWeight) {
        this.initWeight = initWeight;
    }

    public ClientInformation getClient() {
        return client;
    }

    public void setClient(ClientInformation client) {
        this.client = client;
    }

    public int getCurrentWeight() {
        return currentWeight;
    }

    public void setCurrentWeight(int currentWeight) {
        this.currentWeight = currentWeight;
    }
}
