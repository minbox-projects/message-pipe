package org.minbox.framework.message.pipe.server.distribution;

import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.minbox.framework.message.pipe.core.Message;
import org.minbox.framework.message.pipe.core.exception.MessagePipeException;
import org.minbox.framework.message.pipe.core.grpc.MessageServiceGrpc;
import org.minbox.framework.message.pipe.core.grpc.proto.MessageRequest;
import org.minbox.framework.message.pipe.core.grpc.proto.MessageResponse;
import org.minbox.framework.message.pipe.core.information.ClientInformation;
import org.minbox.framework.message.pipe.core.transport.MessageRequestBody;
import org.minbox.framework.message.pipe.core.transport.MessageResponseBody;
import org.minbox.framework.message.pipe.core.transport.MessageResponseStatus;
import org.minbox.framework.message.pipe.core.untis.JsonUtils;
import org.minbox.framework.message.pipe.server.MessagePipe;
import org.minbox.framework.message.pipe.server.config.MessagePipeConfiguration;
import org.minbox.framework.message.pipe.server.service.discovery.ServiceDiscovery;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * Execute messages in the distribution {@link MessagePipe}
 *
 * @author 恒宇少年
 */
@Slf4j
public class MessageDistributionExecutor {
    /**
     * The name of {@link MessagePipe}
     */
    @Getter
    private String pipeName;
    /**
     * The {@link MessagePipe} bound to the current distributor
     */
    @Getter
    private MessagePipe messagePipe;
    private MessagePipeConfiguration configuration;
    private ServiceDiscovery serviceDiscovery;

    public MessageDistributionExecutor(MessagePipe messagePipe,
                                       ServiceDiscovery serviceDiscovery) {
        Assert.notNull(messagePipe, "The MessagePipe cannot be null.");
        Assert.notNull(serviceDiscovery, "The ServiceDiscovery cannot be null.");
        this.messagePipe = messagePipe;
        this.pipeName = messagePipe.getName();
        this.configuration = messagePipe.getConfiguration();
        this.serviceDiscovery = serviceDiscovery;
    }

    /**
     * Waiting for process new messages
     * <p>
     * After discovering a new message from the message pipeline, perform distribution to the client
     * Take the value of {@link MessagePipe#size()}
     * as the judgment condition for processing the distribution message
     */
    public void waitProcessing() {
        while (true) {
            try {
                synchronized (this) {
                    while (this.messagePipe.size() > 0) {
                        try {
                            this.takeAndSend();
                        }
                        // Exit this loop after encountering an exception
                        catch (MessagePipeException e) {
                            log.error(e.getMessage(), e);
                            break;
                        }
                    }
                    // Suspend the current distributor
                    this.wait();
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    /**
     * task a message
     * <p>
     * take and remove the first message from current {@link MessagePipe}
     *
     * @return The {@link Message} instance
     */
    private void takeAndSend() {
        ClientInformation client = serviceDiscovery.lookup(this.pipeName);
        if (ObjectUtils.isEmpty(client)) {
            throw new MessagePipeException("Message Pipe: " + this.pipeName + ", no healthy clients were found.");
        }
        this.messagePipe.handleFirst(message -> sendMessageToClient(message, client));
    }

    /**
     * Send {@link Message} to client
     *
     * @param message The {@link Message} instance
     */
    private boolean sendMessageToClient(Message message, ClientInformation clientInformation) {
        boolean isSendSuccessfully = true;
        String clientId = clientInformation.getClientId();
        ManagedChannel channel = ClientChannelManager.establishChannel(clientInformation);
        try {
            MessageServiceGrpc.MessageServiceBlockingStub messageClientStub = MessageServiceGrpc.newBlockingStub(channel);
            String requestId = this.configuration.getRequestIdGenerator().generate();
            MessageRequestBody requestBody =
                    new MessageRequestBody()
                            .setRequestId(requestId)
                            .setClientId(clientId)
                            .setMessage(message)
                            .setPipeName(this.pipeName);
            String requestJsonBody = JsonUtils.objectToJson(requestBody);
            MessageResponse response = messageClientStub
                    .messageProcessing(MessageRequest.newBuilder().setBody(requestJsonBody).build());
            MessageResponseBody responseBody = JsonUtils.jsonToObject(response.getBody(), MessageResponseBody.class);
            if (!MessageResponseStatus.SUCCESS.equals(responseBody.getStatus())) {
                isSendSuccessfully = false;
                log.error("To the client: {}, " +
                        "the message is sent abnormally, and the message is recovered.", clientId);
            }
        } catch (StatusRuntimeException e) {
            isSendSuccessfully = false;
            Status.Code code = e.getStatus().getCode();
            log.error("To the client: {}, exception when sending a message, Status Code: {}", clientId, code);
            // The server status is UNAVAILABLE
            if (Status.Code.UNAVAILABLE == code) {
                ClientChannelManager.removeChannel(clientId);
                log.error("The client is unavailable, and the cached channel is deleted.");
            }
        } catch (Exception e) {
            isSendSuccessfully = false;
            log.error(e.getMessage(), e);
        }
        if (isSendSuccessfully) {
            log.debug("To the client: {}, sending the message is complete.", clientId);
        }
        return isSendSuccessfully;
    }
}
