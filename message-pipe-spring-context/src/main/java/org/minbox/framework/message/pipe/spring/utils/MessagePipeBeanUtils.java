package org.minbox.framework.message.pipe.spring.utils;

import lombok.extern.slf4j.Slf4j;
import org.minbox.framework.message.pipe.client.MessagePipeClientRunner;
import org.minbox.framework.message.pipe.client.ReceiveMessageService;
import org.minbox.framework.message.pipe.client.process.MessageProcessorManager;
import org.minbox.framework.message.pipe.server.manager.MessagePipeFactoryBean;
import org.minbox.framework.message.pipe.server.manager.MessagePipeLoader;
import org.minbox.framework.message.pipe.server.manager.DefaultMessagePipeManager;
import org.minbox.framework.message.pipe.server.processing.pop.PopMessageFromPipeListener;
import org.minbox.framework.message.pipe.server.processing.push.PushMessageToPipeListener;
import org.minbox.framework.message.pipe.server.service.discovery.ClientServiceDiscovery;
import org.minbox.framework.util.BeanUtils;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.util.ObjectUtils;

/**
 * message pipe bean utils
 *
 * @author 恒宇少年
 */
@Slf4j
public class MessagePipeBeanUtils {
    /**
     * Register message pipe server beans
     *
     * @param registry The {@link BeanDefinitionRegistry} instance
     */
    public static void registerServerBeans(BeanDefinitionRegistry registry) {
        registerMessagePipeFactoryBean(registry);
        registerMessagePipeManager(registry);
        registerMessagePipeLoader(registry);
        registerClientServiceDiscovery(registry);
        registerPushMessageListener(registry);
        registerPopMessageFromPipeListener(registry);
    }

    /**
     * Register message pipe client beans
     *
     * @param registry The {@link BeanDefinitionRegistry} instance
     */
    public static void registerClientBeans(BeanDefinitionRegistry registry) {
        registerMessagePipeClientRunner(registry);
        registerReceiveMessageService(registry);
        registerMessageProcessorManager(registry);
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
     * Register {@link MessagePipeClientRunner}
     *
     * @param registry The {@link BeanDefinitionRegistry} instance
     */
    private static void registerMessagePipeClientRunner(BeanDefinitionRegistry registry) {
        BeanUtils.registerInfrastructureBeanIfAbsent(registry, MessagePipeClientRunner.BEAN_NAME, MessagePipeClientRunner.class);
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
     * Register {@link MessageProcessorManager}
     *
     * @param registry The {@link BeanDefinitionRegistry} instance
     */
    private static void registerMessageProcessorManager(BeanDefinitionRegistry registry) {
        BeanUtils.registerInfrastructureBeanIfAbsent(registry, MessageProcessorManager.BEAN_NAME, MessageProcessorManager.class);
    }

    /**
     * Register {@link MessagePipeLoader}
     *
     * @param registry The {@link BeanDefinitionRegistry} instance
     */
    private static void registerMessagePipeLoader(BeanDefinitionRegistry registry) {
        BeanUtils.registerInfrastructureBeanIfAbsent(registry, MessagePipeLoader.BEAN_NAME, MessagePipeLoader.class);
    }

    /**
     * Register {@link ClientServiceDiscovery}
     *
     * @param registry The {@link BeanDefinitionRegistry} instance
     */
    private static void registerClientServiceDiscovery(BeanDefinitionRegistry registry) {
        BeanUtils.registerInfrastructureBeanIfAbsent(registry, ClientServiceDiscovery.BEAN_NAME, ClientServiceDiscovery.class);
    }

    /**
     * Register {@link PushMessageToPipeListener}
     *
     * @param registry The {@link BeanDefinitionRegistry} instance
     */
    private static void registerPushMessageListener(BeanDefinitionRegistry registry) {
        if (isRegisteredRedisMessageListenerContainer(registry)) {
            BeanUtils.registerInfrastructureBeanIfAbsent(registry, PushMessageToPipeListener.BEAN_NAME, PushMessageToPipeListener.class);
        } else {
            log.warn("No instance of RedisMessageListenerContainer exists and cannot be registered PushMessageToPipeListener.");
        }
    }

    /**
     * Register {@link PopMessageFromPipeListener}
     *
     * @param registry The {@link BeanDefinitionRegistry} instance
     */
    private static void registerPopMessageFromPipeListener(BeanDefinitionRegistry registry) {
        if (isRegisteredRedisMessageListenerContainer(registry)) {
            BeanUtils.registerInfrastructureBeanIfAbsent(registry, PopMessageFromPipeListener.BEAN_NAME, PopMessageFromPipeListener.class);
        } else {
            log.warn("No instance of RedisMessageListenerContainer exists and cannot be registered PopMessageFromPipeListener.");
        }
    }

    /**
     * Verify that {@link RedisMessageListenerContainer} bean exists
     *
     * @param registry The {@link BeanDefinitionRegistry} instance
     * @return return true if exists
     */
    private static boolean isRegisteredRedisMessageListenerContainer(BeanDefinitionRegistry registry) {
        String[] beanNames = ((ListableBeanFactory) registry).getBeanNamesForType(RedisMessageListenerContainer.class);
        return !ObjectUtils.isEmpty(beanNames);
    }
}
