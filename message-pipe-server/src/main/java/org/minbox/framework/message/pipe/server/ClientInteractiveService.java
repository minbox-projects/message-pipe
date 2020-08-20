package org.minbox.framework.message.pipe.server;

import com.alibaba.fastjson.JSON;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.minbox.framework.message.pipe.core.ClientInformation;
import org.minbox.framework.message.pipe.core.ClientStatus;
import org.minbox.framework.message.pipe.core.grpc.ClientServiceGrpc;
import org.minbox.framework.message.pipe.core.grpc.proto.ClientHeartBeatRequest;
import org.minbox.framework.message.pipe.core.grpc.proto.ClientRegisterRequest;
import org.minbox.framework.message.pipe.core.grpc.proto.ClientResponse;
import org.minbox.framework.message.pipe.core.transport.ClientHeartBeatResponseBody;
import org.minbox.framework.message.pipe.core.transport.ClientRegisterResponseBody;
import org.minbox.framework.message.pipe.core.transport.MessageResponseStatus;
import org.minbox.framework.message.pipe.core.untis.StringUtils;
import org.minbox.framework.message.pipe.core.exception.MessagePipeException;

/**
 * Interactive service with client
 *
 * @author 恒宇少年
 */
@Slf4j
public class ClientInteractiveService extends ClientServiceGrpc.ClientServiceImplBase {
    /**
     * The bean name of {@link ClientInteractiveService}
     */
    public static final String BEAN_NAME = "clientInteractiveService";
    private static final String PIPE_NAME_SPLIT_PATTERN = ",";

    /**
     * Register client
     *
     * @param request          client register request {@link ClientRegisterRequest}
     * @param responseObserver stream responseThreadPoolTaskExecutor
     */
    @Override
    public void register(ClientRegisterRequest request, StreamObserver<ClientResponse> responseObserver) {
        ClientRegisterResponseBody responseBody = new ClientRegisterResponseBody();
        try {
            if (StringUtils.isEmpty(request.getAddress()) || StringUtils.isEmpty(request.getMessagePipeName()) ||
                    (request.getPort() <= 0 || request.getPort() > 65535)) {
                throw new MessagePipeException("The client information verification fails and the registration cannot be completed.");
            }
            log.info("Registering client, IP: {}, Port: {}, pipeNames: {}",
                    request.getAddress(), request.getPort(), request.getMessagePipeName());
            String clientId = ClientManager.putIfNotPresent(request.getAddress(), request.getPort());
            responseBody.setClientId(clientId);
            String[] pipeNames = request.getMessagePipeName().split(PIPE_NAME_SPLIT_PATTERN);
            for (String pipeName : pipeNames) {
                ClientManager.bindClientToPipe(pipeName, clientId);
                log.info("Client, Pipe: {}, IP: {}, Port: {}, registration is successful.",
                        pipeName, request.getAddress(), request.getPort());
            }
        } catch (Exception e) {
            responseBody.setStatus(MessageResponseStatus.ERROR);
            log.error("Register client failed.", e);
        }
        String responseBodyJson = JSON.toJSONString(responseBody);
        ClientResponse response = ClientResponse.newBuilder().setBody(responseBodyJson).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    /**
     * Heartbeat check
     *
     * @param request          client heartbeat check request {@link ClientHeartBeatRequest}
     * @param responseObserver stream response
     */
    @Override
    public void heartbeat(ClientHeartBeatRequest request, StreamObserver<ClientResponse> responseObserver) {
        ClientHeartBeatResponseBody responseBody = new ClientHeartBeatResponseBody();
        try {
            if (StringUtils.isEmpty(request.getAddress()) ||
                    (request.getPort() <= 0 || request.getPort() > 65535)) {
                throw new MessagePipeException("The client information that sent the heartbeat is incomplete, " +
                        "and the heartbeat check is ignored this time.");
            }
            Long currentTime = System.currentTimeMillis();
            String clientId = ClientManager.getClientId(request.getAddress(), request.getPort());
            if (!ClientManager.containsClient(clientId)) {
                throw new MessagePipeException("Client: " + clientId + ", not registered.");
            }
            ClientInformation clientInformation = ClientManager.getClient(clientId);
            if (clientInformation.getOnlineTime() <= 0) {
                clientInformation.setOnlineTime(currentTime);
            }
            clientInformation.setLastReportTime(currentTime);
            clientInformation.setStatus(ClientStatus.ON_LINE);
        } catch (Exception e) {
            responseBody.setStatus(MessageResponseStatus.ERROR);
            log.error("Heartbeat check failed.", e);
        }
        String responseBodyJson = JSON.toJSONString(responseBody);
        ClientResponse response = ClientResponse.newBuilder().setBody(responseBodyJson).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
