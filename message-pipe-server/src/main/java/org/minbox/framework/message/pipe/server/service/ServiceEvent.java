package org.minbox.framework.message.pipe.server.service;

import lombok.Getter;
import org.minbox.framework.message.pipe.core.information.ClientInformation;
import org.springframework.context.ApplicationEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Service change event
 * <p>
 * Generally used to subscribe to nacos service changes to publish and update the local client list
 *
 * @author 恒宇少年
 */
@Getter
public class ServiceEvent extends ApplicationEvent {
    /**
     * The type of service event
     */
    private ServiceEventType serviceEventType;
    /**
     * List of affected client information
     */
    private List<ClientInformation> clients = new ArrayList<>();

    /**
     * Constructs a new ServiceEvent instance with event type
     *
     * @param source the event source
     * @param serviceEventType the service event type
     */
    public ServiceEvent(Object source, ServiceEventType serviceEventType) {
        super(source);
        this.serviceEventType = serviceEventType;
    }

    /**
     * Constructs a new ServiceEvent instance with event type and clients
     *
     * @param source the event source
     * @param serviceEventType the service event type
     * @param clients the list of client information
     */
    public ServiceEvent(Object source, ServiceEventType serviceEventType, List<ClientInformation> clients) {
        super(source);
        this.serviceEventType = serviceEventType;
        this.clients = clients;
    }
}
