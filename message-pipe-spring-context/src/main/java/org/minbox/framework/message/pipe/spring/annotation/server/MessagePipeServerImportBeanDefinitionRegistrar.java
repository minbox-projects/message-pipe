package org.minbox.framework.message.pipe.spring.annotation.server;

import org.minbox.framework.message.pipe.spring.utils.MessagePipeBeanUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;

/**
 * @author 恒宇少年
 */
public class MessagePipeServerImportBeanDefinitionRegistrar implements ImportBeanDefinitionRegistrar {
    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        MessagePipeBeanUtils.registerServerBeans(registry);
    }
}
