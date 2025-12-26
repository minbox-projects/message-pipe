package org.minbox.framework.message.pipe.server.service;

import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.listener.Event;
import com.alibaba.nacos.api.naming.listener.EventListener;
import com.alibaba.nacos.api.naming.listener.NamingEvent;
import com.alibaba.nacos.api.naming.pojo.Instance;
import org.minbox.framework.message.pipe.core.PipeConstants;
import org.minbox.framework.message.pipe.core.information.ClientInformation;
import org.minbox.framework.message.pipe.server.service.ServiceEvent;
import org.minbox.framework.message.pipe.server.service.ServiceEventType;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.util.ObjectUtils;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Use nacos to start Server
 * <p>
 * Subscribe to changes in the list of client services in nacos
 *
 * @author 恒宇少年
 */
public class NacosServerApplicationService implements InitializingBean, DisposableBean, EventListener,
        ApplicationEventPublisherAware {
    /**
     * The heartbeat refresh initial delay
     */
    private static final long DEFAULT_HEARTBEAT_REFRESH_INITIAL_DELAY = 5;
    /**
     * The heartbeat refresh period
     */
    private static final long DEFAULT_HEARTBEAT_REFRESH_PERIOD = 5;
    private final NamingService namingService;
    private ApplicationEventPublisher applicationEventPublisher;
    private final ScheduledExecutorService heartbeatRefresher;

    /**
     * Constructs a new NacosServerApplicationService instance
     *
     * @param namingService the Nacos naming service
     */
    public NacosServerApplicationService(NamingService namingService) {
        this.namingService = namingService;
        this.heartbeatRefresher = Executors.newScheduledThreadPool(1);
    }

    @Override
    public void onEvent(Event event) {
        if (!(event instanceof NamingEvent)) {
            return;
        }
        NamingEvent namingEvent = (NamingEvent) event;
        // Publish ServiceChangeEvent
        List<Instance> instances = namingEvent.getInstances();
        List<ClientInformation> clients = instances.stream()
                .filter(instance -> instance.getMetadata().containsKey(PipeConstants.PIPE_NAMES_METADATA_KEY))
                .map(instance -> ClientInformation.valueOf(instance.getIp(), instance.getPort(),
                        instance.getMetadata().get(PipeConstants.PIPE_NAMES_METADATA_KEY)))
                .collect(Collectors.toList());

        if (!ObjectUtils.isEmpty(clients)) {
            ServiceEvent serviceEvent = new ServiceEvent(this, ServiceEventType.RESET_INSTANCE, clients);
            applicationEventPublisher.publishEvent(serviceEvent);
        }
    }

    @Override
    public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    public void destroy() throws Exception {
        this.namingService.shutDown();
        if (this.heartbeatRefresher != null) {
            this.heartbeatRefresher.shutdown();
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        this.namingService.subscribe(PipeConstants.CLIENT_SERVICE_NAME, this);
        // Start heartbeat refresher
        this.heartbeatRefresher.scheduleAtFixedRate(() -> {
            try {
                List<Instance> instances = this.namingService.selectInstances(PipeConstants.CLIENT_SERVICE_NAME, true);
                if (!ObjectUtils.isEmpty(instances)) {
                    List<ClientInformation> clients = instances.stream()
                            .filter(instance -> instance.getMetadata().containsKey(PipeConstants.PIPE_NAMES_METADATA_KEY))
                            .map(instance -> ClientInformation.valueOf(instance.getIp(), instance.getPort(),
                                    instance.getMetadata().get(PipeConstants.PIPE_NAMES_METADATA_KEY)))
                            .collect(Collectors.toList());
                    if (!ObjectUtils.isEmpty(clients)) {
                        ServiceEvent serviceEvent = new ServiceEvent(this, ServiceEventType.HEART_BEAT, clients);
                        applicationEventPublisher.publishEvent(serviceEvent);
                    }
                }
            } catch (Exception e) {
                // Ignore errors
            }
        }, DEFAULT_HEARTBEAT_REFRESH_INITIAL_DELAY, DEFAULT_HEARTBEAT_REFRESH_PERIOD, TimeUnit.SECONDS);
    }
}
