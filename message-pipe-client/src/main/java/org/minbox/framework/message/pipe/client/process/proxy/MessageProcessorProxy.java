package org.minbox.framework.message.pipe.client.process.proxy;

import org.minbox.framework.message.pipe.client.process.MessageProcessor;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;

/**
 * The {@link MessageProcessor} proxy
 *
 * @author 恒宇少年
 */
public class MessageProcessorProxy {
    /**
     * Get {@link MessageProcessor} proxy instance
     *
     * @param clazz Type of proxy
     * @return proxy instance
     */
    public static MessageProcessor getProxy(Class<?> clazz) {
        Enhancer enhancer = new Enhancer();
        MethodInterceptor methodInterceptor = new MessageProcessorMethodInterceptor();
        enhancer.setClassLoader(clazz.getClassLoader());
        enhancer.setSuperclass(clazz);
        enhancer.setCallback(methodInterceptor);
        return (MessageProcessor) enhancer.create();
    }
}
