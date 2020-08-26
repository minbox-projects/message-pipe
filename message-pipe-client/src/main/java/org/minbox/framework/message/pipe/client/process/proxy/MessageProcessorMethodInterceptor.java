package org.minbox.framework.message.pipe.client.process.proxy;

import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;

import java.lang.reflect.Method;

/**
 * Proxy method interceptor
 *
 * @author 恒宇少年
 */
public class MessageProcessorMethodInterceptor implements MethodInterceptor {
    /**
     * handle proxy method
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
        return methodProxy.invokeSuper(o, args);
    }
}
