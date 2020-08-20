package org.minbox.framework.message.pipe.spring.annotation.client;

import org.minbox.framework.message.pipe.spring.utils.MessagePipeBeanUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;

/**
 * Register client beans
 *
 * @author 恒宇少年
 */
public class MessagePipeClientImportBeanDefinitionRegistrar implements ImportBeanDefinitionRegistrar {
    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        MessagePipeBeanUtils.registerClientBeans(registry);
    }
}
