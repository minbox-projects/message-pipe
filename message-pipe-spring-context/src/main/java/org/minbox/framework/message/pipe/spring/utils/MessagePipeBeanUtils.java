package org.minbox.framework.message.pipe.spring.utils;

import org.minbox.framework.message.pipe.client.MessagePipeClientApplication;
import org.minbox.framework.message.pipe.client.ReceiveMessageService;
import org.minbox.framework.message.pipe.client.connect.ConnectServerExecutor;
import org.minbox.framework.message.pipe.client.process.MessageProcessorManager;
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
     * Register message pipe client beans
     *
     * @param registry The {@link BeanDefinitionRegistry} instance
     */
    public static void registerClientBeans(BeanDefinitionRegistry registry) {
        registerMessagePipeClientApplication(registry);
        registerConnectServerExecutor(registry);
        registerReceiveMessageService(registry);
        registerMessageProcessorManager(registry);
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

    /**
     * Register {@link MessagePipeClientApplication}
     *
     * @param registry The {@link BeanDefinitionRegistry} instance
     */
    private static void registerMessagePipeClientApplication(BeanDefinitionRegistry registry) {
        BeanUtils.registerInfrastructureBeanIfAbsent(registry, MessagePipeClientApplication.BEAN_NAME, MessagePipeClientApplication.class);
    }

    /**
     * Register {@link ReceiveMessageService}
     *
     * @param registry The {@link BeanDefinitionRegistry} instance
     */
    private static void registerReceiveMessageService(BeanDefinitionRegistry registry) {
        BeanUtils.registerInfrastructureBeanIfAbsent(registry, ReceiveMessageService.BEAN_NAME, ReceiveMessageService.class);
    }

    /**
     * Register {@link ConnectServerExecutor}
     *
     * @param registry The {@link BeanDefinitionRegistry} instance
     */
    private static void registerConnectServerExecutor(BeanDefinitionRegistry registry) {
        BeanUtils.registerInfrastructureBeanIfAbsent(registry, ConnectServerExecutor.BEAN_NAME, ConnectServerExecutor.class);
    }

    /**
     * Register {@link MessageProcessorManager}
     *
     * @param registry The {@link BeanDefinitionRegistry} instance
     */
    private static void registerMessageProcessorManager(BeanDefinitionRegistry registry) {
        BeanUtils.registerInfrastructureBeanIfAbsent(registry, MessageProcessorManager.BEAN_NAME, MessageProcessorManager.class);
    }
}
