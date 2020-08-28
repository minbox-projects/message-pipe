package org.minbox.framework.message.pipe.spring.annotation.client;

import org.minbox.framework.message.pipe.spring.annotation.client.selector.RegistrarServiceSelector;
import org.minbox.framework.message.pipe.spring.annotation.ServerServiceType;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Enable message pipe client
 * <p>
 * List of beans required for automatic client registration
 *
 * @author 恒宇少年
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Import({MessagePipeClientImportBeanDefinitionRegistrar.class, RegistrarServiceSelector.class})
public @interface EnableMessagePipeClient {
    /**
     * Choose how to register to Server
     *
     * @return {@link ServerServiceType} instance
     * @see org.minbox.framework.message.pipe.client.registrar.RegistrarService
     * @see org.minbox.framework.message.pipe.client.registrar.support.GRpcRegistrarService
     * @see org.minbox.framework.message.pipe.client.registrar.support.NacosRegistrarService
     */
    ServerServiceType serverType() default ServerServiceType.GRPC;
}
