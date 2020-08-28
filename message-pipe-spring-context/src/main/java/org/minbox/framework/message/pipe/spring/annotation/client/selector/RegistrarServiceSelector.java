package org.minbox.framework.message.pipe.spring.annotation.client.selector;

import lombok.extern.slf4j.Slf4j;
import org.minbox.framework.message.pipe.client.registrar.RegistrarService;
import org.minbox.framework.message.pipe.client.registrar.support.GRpcRegistrarService;
import org.minbox.framework.message.pipe.client.registrar.support.NacosRegistrarService;
import org.minbox.framework.message.pipe.core.exception.MessagePipeException;
import org.minbox.framework.message.pipe.spring.annotation.ServerServiceType;
import org.minbox.framework.message.pipe.spring.annotation.client.EnableMessagePipeClient;
import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.type.AnnotationMetadata;

import java.util.Map;

/**
 * The {@link RegistrarService} registrar selector
 * <p>
 * Select the initialized {@link RegistrarService} implementation class instance
 * according to the attribute configuration of the {@link EnableMessagePipeClient} annotation
 * <p>
 * The fully qualified name of the returned class will be automatically registered in the Spring IOC container
 *
 * @author 恒宇少年
 */
@Slf4j
public class RegistrarServiceSelector implements ImportSelector {
    /**
     * The name of {@link ServerServiceType} attributes in {@link EnableMessagePipeClient}
     */
    private static final String REGISTRAR_TYPE_ATTRIBUTE_NAME = "serverType";

    @Override
    public String[] selectImports(AnnotationMetadata importingClassMetadata) {
        Map<String, Object> annotationAttributes =
                importingClassMetadata.getAnnotationAttributes(EnableMessagePipeClient.class.getName());
        ServerServiceType serverServiceType = (ServerServiceType) annotationAttributes.get(REGISTRAR_TYPE_ATTRIBUTE_NAME);
        log.info("Use the [{}] method to register the Client service", serverServiceType);
        switch (serverServiceType) {
            case GRPC:
                return new String[]{GRpcRegistrarService.class.getName()};
            case NACOS:
                return new String[]{NacosRegistrarService.class.getName()};
        }
        throw new MessagePipeException("Unsupported ServerServiceType：" + serverServiceType);
    }
}
