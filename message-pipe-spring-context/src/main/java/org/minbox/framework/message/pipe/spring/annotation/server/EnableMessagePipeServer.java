package org.minbox.framework.message.pipe.spring.annotation.server;

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
@Import(MessagePipeServerImportBeanDefinitionRegistrar.class)
public @interface EnableMessagePipeServer {
    // ....
}
