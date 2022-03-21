package org.minbox.framework.message.pipe.server.service;

import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.listener.Event;
import com.alibaba.nacos.api.naming.listener.EventListener;
import com.alibaba.nacos.api.naming.listener.NamingEvent;
import com.alibaba.nacos.api.naming.pojo.Instance;
import org.minbox.framework.message.pipe.core.PipeConstants;
import org.minbox.framework.message.pipe.core.information.ClientInformation;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.util.ObjectUtils;

import java.util.List;
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
    private NamingService namingService;
    private ApplicationEventPublisher applicationEventPublisher;

    public NacosServerApplicationService(NamingService namingService) {
        this.namingService = namingService;
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
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        this.namingService.subscribe(PipeConstants.CLIENT_SERVICE_NAME, this);
    }
}
