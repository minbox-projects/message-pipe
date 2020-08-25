package org.minbox.framework.message.pipe.client.process;

import lombok.extern.slf4j.Slf4j;
import org.minbox.framework.message.pipe.core.exception.MessagePipeException;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.ObjectUtils;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author 恒宇少年
 */
@Slf4j
public class MessageProcessorManager implements InitializingBean, ApplicationContextAware {
    /**
     * The bean name of {@link MessageProcessorManager}
     */
    public static final String BEAN_NAME = "messageProcessorManager";
    private ApplicationContext applicationContext;
    private Map<String, MessageProcessor> processorMap = new HashMap();

    /**
     * Get {@link MessageProcessor} instance from {@link #processorMap}
     *
     * @param pipeName message pipe name
     * @return message pipe binding {@link MessageProcessor}
     */
    public MessageProcessor getMessageProcessor(String pipeName) {
        MessageProcessor processor = this.regexGetMessageProcessor(pipeName);
        if (ObjectUtils.isEmpty(processor)) {
            throw new MessagePipeException("Message pipeline: " + pipeName + ", there is no bound MessageProcessor.");
        }
        return processor;
    }

    /**
     * @param pipeName
     * @return
     */
    private MessageProcessor regexGetMessageProcessor(String pipeName) {
        Iterator<String> iterator = this.processorMap.keySet().iterator();
        while (iterator.hasNext()) {
            String pipeNamePattern = iterator.next();
            boolean isMatch = Pattern.compile(pipeNamePattern).matcher(pipeName).matches();
            if (isMatch) {
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
        List<String> names = this.processorMap.keySet().stream().collect(Collectors.toList());
        return names.stream().toArray(String[]::new);
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
