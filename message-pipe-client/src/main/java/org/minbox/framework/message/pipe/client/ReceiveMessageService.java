package org.minbox.framework.message.pipe.client;

import com.alibaba.fastjson.JSON;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.minbox.framework.message.pipe.client.config.ClientConfiguration;
import org.minbox.framework.message.pipe.client.process.MessageProcessor;
import org.minbox.framework.message.pipe.core.exception.MessagePipeException;
import org.minbox.framework.message.pipe.core.thread.MessagePipeThreadFactory;
import org.minbox.framework.message.pipe.core.grpc.MessageServiceGrpc;
import org.minbox.framework.message.pipe.core.grpc.proto.MessageRequest;
import org.minbox.framework.message.pipe.core.grpc.proto.MessageResponse;
import org.minbox.framework.message.pipe.core.transport.MessageRequestBody;
import org.minbox.framework.message.pipe.core.transport.MessageResponseBody;
import org.minbox.framework.message.pipe.core.transport.MessageResponseStatus;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.ObjectUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Receive messages from the server and process them
 *
 * @author 恒宇少年
 * @see BeanFactoryAware
 */
@Slf4j
public class ReceiveMessageService extends MessageServiceGrpc.MessageServiceImplBase
        implements InitializingBean, ApplicationContextAware {
    /**
     * The bean name of {@link ReceiveMessageService}
     */
    public static final String BEAN_NAME = "receiveMessageService";
    private ExecutorService executorService;
    private static final String THREAD_NAME_PREFIX = "client";
    private ApplicationContext applicationContext;
    private Map<String, MessageProcessor> processorMap = new HashMap();

    public ReceiveMessageService(ClientConfiguration configuration) {
        this.executorService = Executors.newFixedThreadPool(configuration.getMessageProcessorPoolSize(),
                new MessagePipeThreadFactory(THREAD_NAME_PREFIX));
    }

    @Override
    public void messageProcessing(MessageRequest request, StreamObserver<MessageResponse> responseObserver) {
        executorService.submit(() -> {
            MessageResponseBody responseBody = new MessageResponseBody();
            try {
                String requestJsonBody = request.getBody();
                MessageRequestBody requestBody = JSON.parseObject(requestJsonBody, MessageRequestBody.class);
                String requestId = requestBody.getRequestId();
                requestBody.setRequestId(requestId);
                String pipeName = requestBody.getPipeName();
                byte[] messageBody = requestBody.getMessage().getBody();
                MessageProcessor processor = this.getMessageProcessor(pipeName);
                boolean result = processor.processing(requestId, messageBody);
                responseBody.setStatus(result ? MessageResponseStatus.SUCCESS : MessageResponseStatus.ERROR);
            } catch (Exception e) {
                responseBody.setStatus(MessageResponseStatus.ERROR);
                log.error(e.getMessage(), e);
            } finally {
                MessageResponse response = MessageResponse.newBuilder().setBody(JSON.toJSONString(responseBody)).build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
            }
        });
    }

    /**
     * Get {@link MessageProcessor} instance from {@link #processorMap}
     *
     * @param pipeName message pipe name
     * @return message pipe binding {@link MessageProcessor}
     */
    private MessageProcessor getMessageProcessor(String pipeName) {
        MessageProcessor processor = this.processorMap.get(pipeName);
        if (ObjectUtils.isEmpty(processor)) {
            throw new MessagePipeException("Message pipeline: " + pipeName + ", there is no bound MessageProcessor.");
        }
        return processor;
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
