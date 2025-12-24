package org.minbox.framework.message.pipe.server.manager;

import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import org.minbox.framework.message.pipe.core.Message;
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
 * Message distributor in {@link MessagePipe}
 *
 * @author 恒宇少年
 */
@Slf4j
public class MessagePipeDistributor {
    private final MessagePipe messagePipe;
    private final MessagePipeConfiguration configuration;
    private final ServiceDiscovery serviceDiscovery;
    /**
     * The last time a "no healthy client" log was printed
     */
    private final java.util.concurrent.atomic.AtomicLong lastNoHealthyClientLogTime = new java.util.concurrent.atomic.AtomicLong(0);

    public MessagePipeDistributor(MessagePipe messagePipe, ServiceDiscovery serviceDiscovery) {
        Assert.notNull(messagePipe, "The MessagePipe cannot be null.");
        Assert.notNull(serviceDiscovery, "The ServiceDiscovery cannot be null.");
        this.messagePipe = messagePipe;
        this.configuration = messagePipe.getConfiguration();
        this.serviceDiscovery = serviceDiscovery;
    }

    /**
     * execution send message to client
     *
     * @param message Messages waiting to be distributed
     * @return Whether the message was sent and executed successfully
     */
    public MessageProcessStatus sendMessage(Message message) {
        String pipeName = messagePipe.getName();
        boolean haveHealthClient = serviceDiscovery.checkHaveHealthClient(pipeName);
        if (haveHealthClient) {
            ClientInformation client = serviceDiscovery.lookup(pipeName);
            if (ObjectUtils.isEmpty(client)) {
                return MessageProcessStatus.NO_HEALTH_CLIENT;
            }
            boolean success = this.sendMessageToClient(message, client);
            return success ? MessageProcessStatus.SEND_SUCCESS : MessageProcessStatus.SEND_EXCEPTION;
        } else {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastNoHealthyClientLogTime.get() > 10000) {
                log.warn("Message Pipe [{}], no healthy clients were found，cancel send current message, content：{}.",
                        pipeName, new String(message.getBody()));
                lastNoHealthyClientLogTime.set(currentTime);
            }
            return MessageProcessStatus.NO_HEALTH_CLIENT;
        }
    }

    /**
     * Send {@link Message} to client
     *
     * @param message           The {@link Message} instance
     * @param clientInformation To client information
     * @return @return Whether the message was sent and executed successfully
     */
    private boolean sendMessageToClient(Message message, ClientInformation clientInformation) {
        String clientId = clientInformation.getClientId();
        String pipeName = messagePipe.getName();
        ManagedChannel channel = ClientChannelManager.establishChannel(clientInformation);
        try {
            MessageServiceGrpc.MessageServiceBlockingStub messageClientStub = MessageServiceGrpc.newBlockingStub(channel);
            String requestId = this.configuration.getRequestIdGenerator().generate();
            MessageRequestBody requestBody =
                    new MessageRequestBody()
                            .setRequestId(requestId)
                            .setClientId(clientId)
                            .setMessage(message)
                            .setPipeName(pipeName);
            String requestJsonBody = JsonUtils.objectToJson(requestBody);
            MessageResponse response = messageClientStub
                    .messageProcessing(MessageRequest.newBuilder().setBody(requestJsonBody).build());
            MessageResponseBody responseBody = JsonUtils.jsonToObject(response.getBody(), MessageResponseBody.class);
            if (MessageResponseStatus.SUCCESS.equals(responseBody.getStatus())) {
                log.debug("To the client: {}, sending the message is complete.", clientId);
                return true;
            } else {
                log.error("To the client: {}, the message is sent abnormally.", clientId);
            }
        } catch (StatusRuntimeException e) {
            // Clean up channel immediately on any error
            ClientChannelManager.removeChannel(clientId);
            Status.Code code = e.getStatus().getCode();
            log.error("To the client: {}, exception when sending a message, Status Code: {}", clientId, code);
        } catch (Exception e) {
            log.error("To the client: " + clientId + ", exception when sending a message.", e);
        }
        return false;
    }
}
