package org.minbox.framework.message.pipe.spring.utils;

import org.minbox.framework.message.pipe.server.ClientExpiredExecutor;
import org.minbox.framework.message.pipe.server.ClientInteractiveService;
import org.minbox.framework.message.pipe.server.MessagePipeFactoryBean;
import org.minbox.framework.message.pipe.server.MessagePipeServerApplication;
import org.minbox.framework.message.pipe.server.manager.DefaultMessagePipeManager;
import org.minbox.framework.util.BeanUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;

/**
 * message pipe bean utils
 *
 * @author 恒宇少年
 */
public class MessagePipeBeanUtils {
    /**
     * Register message pipe server beans
     *
     * @param registry The {@link BeanDefinitionRegistry} instance
     */
    public static void registerServerBeans(BeanDefinitionRegistry registry) {
        registerClientExpiredExecutor(registry);
        registerClientInteractiveService(registry);
        registerMessagePipeFactoryBean(registry);
        registerMessagePipeManager(registry);
        registerMessagePipeServerApplication(registry);
    }

    /**
     * Register {@link ClientExpiredExecutor}
     *
     * @param registry The {@link BeanDefinitionRegistry} instance
     */
    private static void registerClientExpiredExecutor(BeanDefinitionRegistry registry) {
        BeanUtils.registerInfrastructureBeanIfAbsent(registry, ClientExpiredExecutor.BEAN_NAME, ClientExpiredExecutor.class);
    }

    /**
     * Register {@link ClientInteractiveService}
     *
     * @param registry The {@link BeanDefinitionRegistry} instance
     */
    private static void registerClientInteractiveService(BeanDefinitionRegistry registry) {
        BeanUtils.registerInfrastructureBeanIfAbsent(registry, ClientInteractiveService.BEAN_NAME, ClientInteractiveService.class);
    }

    /**
     * Register {@link MessagePipeFactoryBean}
     *
     * @param registry The {@link BeanDefinitionRegistry} instance
     */
    private static void registerMessagePipeFactoryBean(BeanDefinitionRegistry registry) {
        BeanUtils.registerInfrastructureBeanIfAbsent(registry, MessagePipeFactoryBean.BEAN_NAME, MessagePipeFactoryBean.class);
    }

    /**
     * Register {@link DefaultMessagePipeManager}
     *
     * @param registry The {@link BeanDefinitionRegistry} instance
     */
    private static void registerMessagePipeManager(BeanDefinitionRegistry registry) {
        BeanUtils.registerInfrastructureBeanIfAbsent(registry, DefaultMessagePipeManager.BEAN_NAME, DefaultMessagePipeManager.class);
    }

    /**
     * Register {@link MessagePipeServerApplication}
     *
     * @param registry The {@link BeanDefinitionRegistry} instance
     */
    private static void registerMessagePipeServerApplication(BeanDefinitionRegistry registry) {
        BeanUtils.registerInfrastructureBeanIfAbsent(registry, MessagePipeServerApplication.BEAN_NAME, MessagePipeServerApplication.class);
    }
}
