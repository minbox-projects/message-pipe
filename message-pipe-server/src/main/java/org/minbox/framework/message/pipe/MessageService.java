package org.minbox.framework.message.pipe;

import com.alibaba.fastjson.JSON;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.minbox.framework.message.pipe.core.Message;
import org.minbox.framework.message.pipe.core.grpc.MessageRequest;
import org.minbox.framework.message.pipe.core.grpc.MessageResponse;
import org.minbox.framework.message.pipe.core.grpc.MessageServiceGrpc;
import org.minbox.framework.message.pipe.core.transport.MessageRequestBody;
import org.minbox.framework.message.pipe.core.transport.MessageResponseBody;
import org.minbox.framework.message.pipe.core.transport.MessageResponseStatus;
import org.minbox.framework.message.pipe.exception.MessagePipeException;
import org.minbox.framework.message.pipe.manager.MessagePipeManager;

/**
 * Message Service grpc implement class
 * <p>
 * Rewrite the {@link MessageServiceGrpc.MessageServiceImplBase#takeMessage} method to complete the message acquisition logic
 *
 * @author 恒宇少年
 * @see MessageServiceGrpc.MessageServiceImplBase
 */
@Slf4j
public class MessageService extends MessageServiceGrpc.MessageServiceImplBase {
    /**
     * The {@link MessagePipe} manager
     *
     * @see org.minbox.framework.message.pipe.manager.DefaultMessagePipeManager
     */
    private MessagePipeManager manager;

    public MessageService(MessagePipeManager manager) {
        this.manager = manager;
    }

    /**
     * Take a message from {@link MessagePipe}
     *
     * @param request          The grpc {@link MessageRequest} instance
     * @param responseObserver response stream object
     */
    @Override
    public void takeMessage(MessageRequest request, StreamObserver<MessageResponse> responseObserver) {
        String requestJsonBody = request.getBody();
        MessageRequestBody requestBody = this.convertMessage(requestJsonBody);
        MessageResponse response = null;
        try {
            MessagePipe messagePipe = this.manager.getMessagePipe(requestBody.getPipeName());
            Message message = messagePipe.take();
            if (message == null) {
                throw new MessagePipeException("MessagePipe：" + requestBody.getPipeName() + "，" +
                        "message not obtained, this time ignore.");
            }
            response = this.buildResponseBody(requestBody.getRequestId(), message);
        } catch (Exception e) {
            response = this.buildErrorResponseBody(requestBody.getRequestId());
            log.error(e.getMessage(), e);
        } finally {
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }

    /**
     * Build {@link MessageResponse} instance
     * <p>
     * The {@link MessageResponseStatus} is default {@link MessageResponseStatus#SUCCESS}
     *
     * @param requestId {@link MessageRequestBody#getRequestId()}
     * @param message   Response message {@link Message}
     * @return The {@link MessageResponse} instance
     */
    private MessageResponse buildResponseBody(String requestId, Message message) {
        MessageResponseBody responseBody = new MessageResponseBody()
                .setRequestId(requestId)
                .setMessage(message)
                .setStatus(MessageResponseStatus.SUCCESS);
        return MessageResponse.newBuilder().setBody(JSON.toJSONString(responseBody)).build();
    }

    /**
     * Build error status {@link MessageResponse} instance
     *
     * @param requestId {@link MessageRequestBody#getRequestId()}
     * @return The {@link MessageResponse} instance
     */
    private MessageResponse buildErrorResponseBody(String requestId) {
        MessageResponseBody responseBody = new MessageResponseBody()
                .setRequestId(requestId)
                .setStatus(MessageResponseStatus.ERROR);
        return MessageResponse.newBuilder().setBody(JSON.toJSONString(responseBody)).build();
    }

    /**
     * Convert request json string to {@link MessageRequestBody}
     *
     * @param requestJsonBody {@link MessageRequest#getBody()}
     * @return {@link MessageRequestBody} instance
     */
    private MessageRequestBody convertMessage(String requestJsonBody) {
        try {
            return JSON.parseObject(requestJsonBody, MessageRequestBody.class);
        } catch (Exception e) {
            throw new MessagePipeException("Convert json request body to MessageRequestBody failed.", e);
        }
    }
}
