package org.minbox.framework.message.pipe.client;

import com.alibaba.fastjson.JSON;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.minbox.framework.message.pipe.client.process.MessageProcessor;
import org.minbox.framework.message.pipe.client.process.MessageProcessorManager;
import org.minbox.framework.message.pipe.core.grpc.MessageServiceGrpc;
import org.minbox.framework.message.pipe.core.grpc.proto.MessageRequest;
import org.minbox.framework.message.pipe.core.grpc.proto.MessageResponse;
import org.minbox.framework.message.pipe.core.transport.MessageRequestBody;
import org.minbox.framework.message.pipe.core.transport.MessageResponseBody;
import org.minbox.framework.message.pipe.core.transport.MessageResponseStatus;
import org.springframework.beans.factory.BeanFactoryAware;

/**
 * Receive messages from the server and process them
 *
 * @author 恒宇少年
 * @see BeanFactoryAware
 */
@Slf4j
public class ReceiveMessageService extends MessageServiceGrpc.MessageServiceImplBase {
    /**
     * The bean name of {@link ReceiveMessageService}
     */
    public static final String BEAN_NAME = "receiveMessageService";
    private MessageProcessorManager messageProcessorManager;

    public ReceiveMessageService(MessageProcessorManager messageProcessorManager) {
        this.messageProcessorManager = messageProcessorManager;
    }

    @Override
    public void messageProcessing(MessageRequest request, StreamObserver<MessageResponse> responseObserver) {
        MessageResponseBody responseBody = new MessageResponseBody();
        try {
            String requestJsonBody = request.getBody();
            MessageRequestBody requestBody = JSON.parseObject(requestJsonBody, MessageRequestBody.class);
            String requestId = requestBody.getRequestId();
            requestBody.setRequestId(requestId);
            String pipeName = requestBody.getPipeName();
            byte[] messageBody = requestBody.getMessage().getBody();
            MessageProcessor processor = messageProcessorManager.getMessageProcessor(pipeName);
            boolean result = processor.processing(pipeName, requestId, messageBody);
            responseBody.setStatus(result ? MessageResponseStatus.SUCCESS : MessageResponseStatus.ERROR);
        } catch (Exception e) {
            responseBody.setStatus(MessageResponseStatus.ERROR);
            log.error(e.getMessage(), e);
        } finally {
            MessageResponse response = MessageResponse.newBuilder().setBody(JSON.toJSONString(responseBody)).build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }
}
