package org.minbox.framework.message.pipe.spring.annotation.client;

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
@Import(MessagePipeClientImportBeanDefinitionRegistrar.class)
public @interface EnableMessagePipeClient {
    // ...
}
