package org.minbox.framework.message.pipe.spring.annotation.server;

import org.minbox.framework.message.pipe.spring.annotation.ServerServiceType;
import org.minbox.framework.message.pipe.spring.annotation.server.selector.ServerApplicationServiceSelector;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Enable message pipe server
 * <p>
 * Register the internal configuration classes required by the server to spring ioc through @import annotation
 *
 * @author 恒宇少年
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Import({MessagePipeServerImportBeanDefinitionRegistrar.class, ServerApplicationServiceSelector.class})
public @interface EnableMessagePipeServer {
    /**
     * Configure the way to pull the client
     *
     * @return {@link ServerServiceType} instance
     * @see org.minbox.framework.message.pipe.client.registrar.RegistrarService
     * @see org.minbox.framework.message.pipe.client.registrar.support.GRpcRegistrarService
     * @see org.minbox.framework.message.pipe.client.registrar.support.NacosRegistrarService
     */
    ServerServiceType serverType() default ServerServiceType.GRPC;
}
