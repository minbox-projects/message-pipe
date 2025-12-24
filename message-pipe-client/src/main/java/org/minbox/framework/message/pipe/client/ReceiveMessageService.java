package org.minbox.framework.message.pipe.client;

import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.minbox.framework.message.pipe.client.process.MessageProcessor;
import org.minbox.framework.message.pipe.client.process.MessageProcessorManager;
import org.minbox.framework.message.pipe.core.Message;
import org.minbox.framework.message.pipe.core.grpc.MessageServiceGrpc;
import org.minbox.framework.message.pipe.core.grpc.proto.MessageRequest;
import org.minbox.framework.message.pipe.core.grpc.proto.MessageResponse;
import org.minbox.framework.message.pipe.core.transport.MessageRequestBody;
import org.minbox.framework.message.pipe.core.transport.MessageResponseBody;
import org.minbox.framework.message.pipe.core.transport.MessageResponseStatus;
import org.minbox.framework.message.pipe.core.untis.JsonUtils;
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
    private final MessageProcessorManager messageProcessorManager;

    public ReceiveMessageService(MessageProcessorManager messageProcessorManager) {
        this.messageProcessorManager = messageProcessorManager;
    }

    @Override
    public void messageProcessing(MessageRequest request, StreamObserver<MessageResponse> responseObserver) {
        MessageResponseBody responseBody = new MessageResponseBody();
        try {
            String requestJsonBody = request.getBody();
            MessageRequestBody requestBody = JsonUtils.jsonToObject(requestJsonBody, MessageRequestBody.class);
            String requestId = requestBody.getRequestId();
            requestBody.setRequestId(requestId);
            String pipeName = requestBody.getPipeName();
            
            java.util.List<Message> messages = requestBody.getMessages();
            
            if (messages != null && !messages.isEmpty()) {
                // Batch processing
                int successCount = 0;
                boolean batchFailed = false;
                MessageProcessor processor = messageProcessorManager.getMessageProcessor(pipeName);
                
                for (Message message : messages) {
                    try {
                        boolean result = processor.processing(pipeName, requestId, message);
                        if (result) {
                            successCount++;
                        } else {
                            batchFailed = true;
                            break;
                        }
                    } catch (Exception e) {
                        batchFailed = true;
                        log.error("Error processing message in batch", e);
                        break;
                    }
                }
                
                responseBody.setSuccessCount(successCount);
                responseBody.setStatus(batchFailed ? MessageResponseStatus.ERROR : MessageResponseStatus.SUCCESS);
                
            } else {
                log.warn("Received empty message batch for pipe: {}", pipeName);
                responseBody.setStatus(MessageResponseStatus.SUCCESS);
                responseBody.setSuccessCount(0);
            }
        } catch (Exception e) {
            responseBody.setStatus(MessageResponseStatus.ERROR);
            log.error(e.getMessage(), e);
        } finally {
            String responseJsonBody = JsonUtils.objectToJson(responseBody);
            MessageResponse response = MessageResponse.newBuilder().setBody(responseJsonBody).build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }
}
