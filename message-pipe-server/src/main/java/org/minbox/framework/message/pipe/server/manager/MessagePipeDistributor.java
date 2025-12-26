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

import java.util.List;
import java.util.concurrent.TimeUnit;



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
     * Check if there are any healthy clients for this pipe
     *
     * @return true if at least one healthy client exists
     */
    public boolean hasHealthyClient() {
        return serviceDiscovery.checkHaveHealthClient(messagePipe.getName());
    }

    /**
     * Resolve a client for this pipe
     *
     * @return The resolved client information
     */
    public ClientInformation resolveClient() {
        return serviceDiscovery.lookup(messagePipe.getName());
    }

    /**
     * Send a batch of messages to a client
     *
     * @param messages List of messages
     * @return Number of successfully processed messages. Returns -1 if communication failed.
     */
    public int sendMessageBatch(List<Message> messages) {
        ClientInformation client = this.resolveClient();
        if (ObjectUtils.isEmpty(client)) {
            return -1;
        }
        boolean hasHealthyClient = this.hasHealthyClient();
        if (!hasHealthyClient) {
            return -1;
        }
        String clientId = client.getClientId();
        String pipeName = messagePipe.getName();
        ManagedChannel channel = ClientChannelManager.establishChannel(client);
        try {
            MessageServiceGrpc.MessageServiceBlockingStub messageClientStub = MessageServiceGrpc.newBlockingStub(channel)
                    .withDeadlineAfter(configuration.getMessageRequestTimeoutMillis(), TimeUnit.MILLISECONDS);
            String requestId = this.configuration.getRequestIdGenerator().generate();
            MessageRequestBody requestBody =
                    new MessageRequestBody()
                            .setRequestId(requestId)
                            .setClientId(clientId)
                            .setMessages(messages)
                            .setPipeName(pipeName);
            String requestJsonBody = JsonUtils.objectToJson(requestBody);
            MessageResponse response = messageClientStub
                    .messageProcessing(MessageRequest.newBuilder().setBody(requestJsonBody).build());
            MessageResponseBody responseBody = JsonUtils.jsonToObject(response.getBody(), MessageResponseBody.class);

            // Return the count reported by client
            // If client is old version, it might return 0 successCount but status SUCCESS.
            // We should handle compatibility if needed, but assuming client is updated.
            if (responseBody == null) {
                return -1;
            }
            if (MessageResponseStatus.SUCCESS.equals(responseBody.getStatus())) {
                int count = responseBody.getSuccessCount();
                int successCount = count > 0 ? count : messages.size();
                // Record stats
                MessagePipeMetricsAggregator.getInstance().recordClientActivity(clientId, successCount, messages.size() - successCount);
                return successCount;
            } else {
                int successCount = responseBody.getSuccessCount();
                // Record stats
                MessagePipeMetricsAggregator.getInstance().recordClientActivity(clientId, successCount, messages.size() - successCount);
                return successCount;
            }
        } catch (StatusRuntimeException e) {
            ClientChannelManager.removeChannel(clientId);
            // Record failure stats
            MessagePipeMetricsAggregator.getInstance().recordClientActivity(clientId, 0, messages.size());
            
            // Only exclude client if it is unavailable or timed out
            // For DEADLINE_EXCEEDED (30s timeout), we mark offline to allow recovery via heartbeat
            if (Status.Code.UNAVAILABLE == e.getStatus().getCode() || 
                Status.Code.DEADLINE_EXCEEDED == e.getStatus().getCode()) {
                serviceDiscovery.exclude(clientId);
            }
            log.error("To the client: {}, batch send exception, Status Code: {}", clientId, e.getStatus().getCode());
        } catch (Exception e) {
            // Record failure stats
            MessagePipeMetricsAggregator.getInstance().recordClientActivity(clientId, 0, messages.size());
            log.error("To the client: " + clientId + ", batch send exception.", e);
        }
        return -1; // Network/System error
    }
}
