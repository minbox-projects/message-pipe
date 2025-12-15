package org.minbox.framework.message.pipe.client.process.proxy;

import org.minbox.framework.message.pipe.client.process.MessageProcessor;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;

import java.lang.reflect.Method;

/**
 * Proxy method interceptor
 *
 * <p>
 * This interceptor delegates method calls to the original Spring Bean instance.
 * By delegating to the original instance, we ensure that all Spring dependency
 * injections (@Autowired, @Resource, etc.) are properly preserved and accessible.
 * </p>
 *
 * @author 恒宇少年
 */
public class MessageProcessorMethodInterceptor implements MethodInterceptor {
    /**
     * The original MessageProcessor instance managed by Spring
     */
    private final MessageProcessor target;

    /**
     * Create the interceptor with the original Spring Bean instance
     *
     * @param target The original MessageProcessor instance
     */
    public MessageProcessorMethodInterceptor(MessageProcessor target) {
        this.target = target;
    }

    /**
     * Intercept method calls and delegate to the original instance
     *
     * @param o           The proxy object instance
     * @param method      source method
     * @param args        method args
     * @param methodProxy proxy method
     * @return proxy method result
     * @throws Throwable Exception encountered
     */
    @Override
    public Object intercept(Object o, Method method, Object[] args, MethodProxy methodProxy) throws Throwable {
        // Delegate to the original Spring Bean instance
        // This ensures all @Autowired dependencies are available
        return method.invoke(target, args);
    }
}
