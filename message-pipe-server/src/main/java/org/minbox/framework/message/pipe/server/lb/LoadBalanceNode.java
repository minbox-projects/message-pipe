package org.minbox.framework.message.pipe.server.lb;

import org.minbox.framework.message.pipe.core.information.ClientInformation;

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

    /**
     * Constructs a new LoadBalanceNode instance
     *
     * @param client the client information
     */
    public LoadBalanceNode(ClientInformation client) {
        this.client = client;
    }

    /**
     * Gets the init weight
     *
     * @return the init weight
     */
    public int getInitWeight() {
        return initWeight;
    }

    /**
     * Sets the init weight
     *
     * @param initWeight the init weight to set
     */
    public void setInitWeight(int initWeight) {
        this.initWeight = initWeight;
    }

    /**
     * Gets the client information
     *
     * @return the client information
     */
    public ClientInformation getClient() {
        return client;
    }

    /**
     * Sets the client information
     *
     * @param client the client information to set
     */
    public void setClient(ClientInformation client) {
        this.client = client;
    }

    /**
     * Gets the current weight
     *
     * @return the current weight
     */
    public int getCurrentWeight() {
        return currentWeight;
    }

    /**
     * Sets the current weight
     *
     * @param currentWeight the current weight to set
     */
    public void setCurrentWeight(int currentWeight) {
        this.currentWeight = currentWeight;
    }
}
