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
     * Get {@link MessageProcessor} proxy instance for the original Spring Bean
     *
     * <p>
     * This method creates a CGLIB proxy for the original MessageProcessor instance.
     * The proxy delegates method calls to the original Spring Bean instance, ensuring
     * that all Spring dependency injections (@Autowired, etc.) are properly preserved.
     * </p>
     *
     * @param target The original MessageProcessor instance managed by Spring
     * @return proxy instance that delegates to the original instance
     */
    public static MessageProcessor getProxy(MessageProcessor target) {
        Enhancer enhancer = new Enhancer();
        enhancer.setClassLoader(target.getClass().getClassLoader());
        enhancer.setSuperclass(target.getClass());
        // Pass the original instance to the interceptor so it can delegate calls
        enhancer.setCallback(new MessageProcessorMethodInterceptor(target));
        return (MessageProcessor) enhancer.create();
    }
}
