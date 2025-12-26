package org.minbox.framework.message.pipe.client.process;

import lombok.extern.slf4j.Slf4j;
import org.minbox.framework.message.pipe.client.process.proxy.MessageProcessorProxy;
import org.minbox.framework.message.pipe.core.PipeConstants;
import org.minbox.framework.message.pipe.core.exception.MessagePipeException;
import org.minbox.framework.message.pipe.core.untis.RegexUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * The {@link MessageProcessor} manager
 * <p>
 * when the project starts
 * it will get the list of MessageProcessor implementation class instances registered in the Spring Ioc container
 *
 * @author 恒宇少年
 */
@Slf4j
public class MessageProcessorManager implements InitializingBean, ApplicationContextAware {
    /**
     * The bean name of {@link MessageProcessorManager}
     */
    public static final String BEAN_NAME = "messageProcessorManager";
    private ApplicationContext applicationContext;
    private final ConcurrentMap<String, MessageProcessor> processorMap = new ConcurrentHashMap<>();

    /**
     * Get {@link MessageProcessor} instance from {@link #processorMap}
     *
     * <p>
     * For REGEX type MessageProcessors, this method creates a CGLIB proxy instance
     * that delegates to the original Spring Bean instance. This ensures that:
     * 1. All Spring dependency injections (@Autowired, etc.) are properly preserved
     * 2. The same Spring Bean instance is used for all matching pipeline names
     * 3. Proxy objects are cached to avoid redundant creation
     * 4. The solution is thread-safe with atomic cache operations
     * </p>
     *
     * @param pipeName message pipe name
     * @return message pipe binding {@link MessageProcessor}
     */
    public synchronized MessageProcessor getMessageProcessor(String pipeName) {
        MessageProcessor processor = this.regexGetMessageProcessor(pipeName);
        if (ObjectUtils.isEmpty(processor)) {
            throw new MessagePipeException("Message pipeline: " + pipeName + ", there is no bound MessageProcessor.");
        }
        // For REGEX type, use computeIfAbsent to atomically create and cache the proxy instance
        // This prevents race conditions and ensures only one proxy is created per pipeline name
        if (MessageProcessorType.REGEX == processor.processorType()) {
            return this.processorMap.computeIfAbsent(pipeName, key -> {
                // Pass the original Spring Bean instance to create the proxy
                // The proxy will delegate all method calls to the original instance
                return MessageProcessorProxy.getProxy(processor);
            });
        }
        return processor;
    }

    /**
     * Get the message processor matched by the regular expression according to the pipe name
     *
     * @param pipeName Specific message pipe name
     * @return The {@link MessageProcessor} instance
     */
    private MessageProcessor regexGetMessageProcessor(String pipeName) {
        for (String pipeNamePattern : this.processorMap.keySet()) {
            if (RegexUtils.isMatch(pipeNamePattern, pipeName)) {
                return this.processorMap.get(pipeNamePattern);
            }
        }
        return null;
    }

    /**
     * Get client binding all pipe names
     *
     * @return The pipe name array
     */
    public String[] getBindingPipeNames() {
        List<String> names = new ArrayList<>(this.processorMap.keySet());
        return names.toArray(String[]::new);
    }

    /**
     * Convert the String array into a string separated by ","
     * <p>
     * example：
     * 1,2,3,4,5
     *
     * @return pipeNames
     */
    public String getBindingPipeNameString() {
        return StringUtils.arrayToDelimitedString(this.getBindingPipeNames(), PipeConstants.PIPE_NAME_SPLIT);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Map<String, MessageProcessor> beans = this.applicationContext.getBeansOfType(MessageProcessor.class);
        if (ObjectUtils.isEmpty(beans)) {
            log.warn("No MessageProcessor instance is defined.");
        } else {
            beans.keySet().stream().forEach(beanName -> {
                MessageProcessor processor = beans.get(beanName);
                this.processorMap.put(processor.bindingPipeName(), processor);
            });
        }
    }
}
