package org.minbox.framework.message.pipe.spring.annotation.server.selector;

import lombok.extern.slf4j.Slf4j;
import org.minbox.framework.message.pipe.core.exception.MessagePipeException;
import org.minbox.framework.message.pipe.server.service.GRpcServerApplicationService;
import org.minbox.framework.message.pipe.server.service.NacosServerApplicationService;
import org.minbox.framework.message.pipe.spring.annotation.ServerServiceType;
import org.minbox.framework.message.pipe.spring.annotation.server.EnableMessagePipeServer;
import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.type.AnnotationMetadata;

import java.util.Map;

/**
 * Choose to register ServerApplicationService
 *
 * @author 恒宇少年
 * @see GRpcServerApplicationService
 * @see NacosServerApplicationService
 */
@Slf4j
public class ServerApplicationServiceSelector implements ImportSelector {
    /**
     * The name of {@link ServerServiceType} attributes in {@link EnableMessagePipeServer}
     */
    private static final String REGISTRAR_TYPE_ATTRIBUTE_NAME = "serverType";

    @Override
    public String[] selectImports(AnnotationMetadata importingClassMetadata) {
        Map<String, Object> annotationAttributes =
                importingClassMetadata.getAnnotationAttributes(EnableMessagePipeServer.class.getName());
        ServerServiceType serverServiceType = (ServerServiceType) annotationAttributes.get(REGISTRAR_TYPE_ATTRIBUTE_NAME);
        log.info("MessagePipe server startup mode：[{}].", serverServiceType);
        switch (serverServiceType) {
            case GRPC:
                return new String[]{GRpcServerApplicationService.class.getName()};
            case NACOS:
                return new String[]{NacosServerApplicationService.class.getName()};
        }
        throw new MessagePipeException("Unsupported ServerServiceType：" + serverServiceType);
    }
}
