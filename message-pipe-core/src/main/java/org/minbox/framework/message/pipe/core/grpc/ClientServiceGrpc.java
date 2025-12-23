package org.minbox.framework.message.pipe.core.grpc;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.45.1)",
    comments = "Source: ClientService.proto")
@io.grpc.stub.annotations.GrpcGenerated
public final class ClientServiceGrpc {

  private ClientServiceGrpc() {}

  public static final String SERVICE_NAME = "org.minbox.framework.message.pipe.core.grpc.ClientService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<org.minbox.framework.message.pipe.core.grpc.ClientRegisterRequest,
      org.minbox.framework.message.pipe.core.grpc.ClientResponse> getRegisterMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "register",
      requestType = org.minbox.framework.message.pipe.core.grpc.ClientRegisterRequest.class,
      responseType = org.minbox.framework.message.pipe.core.grpc.ClientResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<org.minbox.framework.message.pipe.core.grpc.ClientRegisterRequest,
      org.minbox.framework.message.pipe.core.grpc.ClientResponse> getRegisterMethod() {
    io.grpc.MethodDescriptor<org.minbox.framework.message.pipe.core.grpc.ClientRegisterRequest, org.minbox.framework.message.pipe.core.grpc.ClientResponse> getRegisterMethod;
    if ((getRegisterMethod = ClientServiceGrpc.getRegisterMethod) == null) {
      synchronized (ClientServiceGrpc.class) {
        if ((getRegisterMethod = ClientServiceGrpc.getRegisterMethod) == null) {
          ClientServiceGrpc.getRegisterMethod = getRegisterMethod =
              io.grpc.MethodDescriptor.<org.minbox.framework.message.pipe.core.grpc.ClientRegisterRequest, org.minbox.framework.message.pipe.core.grpc.ClientResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "register"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.minbox.framework.message.pipe.core.grpc.ClientRegisterRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.minbox.framework.message.pipe.core.grpc.ClientResponse.getDefaultInstance()))
              .setSchemaDescriptor(new ClientServiceMethodDescriptorSupplier("register"))
              .build();
        }
      }
    }
    return getRegisterMethod;
  }

  private static volatile io.grpc.MethodDescriptor<org.minbox.framework.message.pipe.core.grpc.ClientHeartBeatRequest,
      org.minbox.framework.message.pipe.core.grpc.ClientResponse> getHeartbeatMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "heartbeat",
      requestType = org.minbox.framework.message.pipe.core.grpc.ClientHeartBeatRequest.class,
      responseType = org.minbox.framework.message.pipe.core.grpc.ClientResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<org.minbox.framework.message.pipe.core.grpc.ClientHeartBeatRequest,
      org.minbox.framework.message.pipe.core.grpc.ClientResponse> getHeartbeatMethod() {
    io.grpc.MethodDescriptor<org.minbox.framework.message.pipe.core.grpc.ClientHeartBeatRequest, org.minbox.framework.message.pipe.core.grpc.ClientResponse> getHeartbeatMethod;
    if ((getHeartbeatMethod = ClientServiceGrpc.getHeartbeatMethod) == null) {
      synchronized (ClientServiceGrpc.class) {
        if ((getHeartbeatMethod = ClientServiceGrpc.getHeartbeatMethod) == null) {
          ClientServiceGrpc.getHeartbeatMethod = getHeartbeatMethod =
              io.grpc.MethodDescriptor.<org.minbox.framework.message.pipe.core.grpc.ClientHeartBeatRequest, org.minbox.framework.message.pipe.core.grpc.ClientResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "heartbeat"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.minbox.framework.message.pipe.core.grpc.ClientHeartBeatRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.minbox.framework.message.pipe.core.grpc.ClientResponse.getDefaultInstance()))
              .setSchemaDescriptor(new ClientServiceMethodDescriptorSupplier("heartbeat"))
              .build();
        }
      }
    }
    return getHeartbeatMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static ClientServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<ClientServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<ClientServiceStub>() {
        @java.lang.Override
        public ClientServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new ClientServiceStub(channel, callOptions);
        }
      };
    return ClientServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static ClientServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<ClientServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<ClientServiceBlockingStub>() {
        @java.lang.Override
        public ClientServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new ClientServiceBlockingStub(channel, callOptions);
        }
      };
    return ClientServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static ClientServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<ClientServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<ClientServiceFutureStub>() {
        @java.lang.Override
        public ClientServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new ClientServiceFutureStub(channel, callOptions);
        }
      };
    return ClientServiceFutureStub.newStub(factory, channel);
  }

  /**
   */
  public static abstract class ClientServiceImplBase implements io.grpc.BindableService {

    /**
     */
    public void register(org.minbox.framework.message.pipe.core.grpc.ClientRegisterRequest request,
        io.grpc.stub.StreamObserver<org.minbox.framework.message.pipe.core.grpc.ClientResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getRegisterMethod(), responseObserver);
    }

    /**
     */
    public void heartbeat(org.minbox.framework.message.pipe.core.grpc.ClientHeartBeatRequest request,
        io.grpc.stub.StreamObserver<org.minbox.framework.message.pipe.core.grpc.ClientResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getHeartbeatMethod(), responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            getRegisterMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                org.minbox.framework.message.pipe.core.grpc.ClientRegisterRequest,
                org.minbox.framework.message.pipe.core.grpc.ClientResponse>(
                  this, METHODID_REGISTER)))
          .addMethod(
            getHeartbeatMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                org.minbox.framework.message.pipe.core.grpc.ClientHeartBeatRequest,
                org.minbox.framework.message.pipe.core.grpc.ClientResponse>(
                  this, METHODID_HEARTBEAT)))
          .build();
    }
  }

  /**
   */
  public static final class ClientServiceStub extends io.grpc.stub.AbstractAsyncStub<ClientServiceStub> {
    private ClientServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected ClientServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new ClientServiceStub(channel, callOptions);
    }

    /**
     */
    public void register(org.minbox.framework.message.pipe.core.grpc.ClientRegisterRequest request,
        io.grpc.stub.StreamObserver<org.minbox.framework.message.pipe.core.grpc.ClientResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getRegisterMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void heartbeat(org.minbox.framework.message.pipe.core.grpc.ClientHeartBeatRequest request,
        io.grpc.stub.StreamObserver<org.minbox.framework.message.pipe.core.grpc.ClientResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getHeartbeatMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   */
  public static final class ClientServiceBlockingStub extends io.grpc.stub.AbstractBlockingStub<ClientServiceBlockingStub> {
    private ClientServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected ClientServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new ClientServiceBlockingStub(channel, callOptions);
    }

    /**
     */
    public org.minbox.framework.message.pipe.core.grpc.ClientResponse register(org.minbox.framework.message.pipe.core.grpc.ClientRegisterRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getRegisterMethod(), getCallOptions(), request);
    }

    /**
     */
    public org.minbox.framework.message.pipe.core.grpc.ClientResponse heartbeat(org.minbox.framework.message.pipe.core.grpc.ClientHeartBeatRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getHeartbeatMethod(), getCallOptions(), request);
    }
  }

  /**
   */
  public static final class ClientServiceFutureStub extends io.grpc.stub.AbstractFutureStub<ClientServiceFutureStub> {
    private ClientServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected ClientServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new ClientServiceFutureStub(channel, callOptions);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<org.minbox.framework.message.pipe.core.grpc.ClientResponse> register(
        org.minbox.framework.message.pipe.core.grpc.ClientRegisterRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getRegisterMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<org.minbox.framework.message.pipe.core.grpc.ClientResponse> heartbeat(
        org.minbox.framework.message.pipe.core.grpc.ClientHeartBeatRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getHeartbeatMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_REGISTER = 0;
  private static final int METHODID_HEARTBEAT = 1;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final ClientServiceImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(ClientServiceImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_REGISTER:
          serviceImpl.register((org.minbox.framework.message.pipe.core.grpc.ClientRegisterRequest) request,
              (io.grpc.stub.StreamObserver<org.minbox.framework.message.pipe.core.grpc.ClientResponse>) responseObserver);
          break;
        case METHODID_HEARTBEAT:
          serviceImpl.heartbeat((org.minbox.framework.message.pipe.core.grpc.ClientHeartBeatRequest) request,
              (io.grpc.stub.StreamObserver<org.minbox.framework.message.pipe.core.grpc.ClientResponse>) responseObserver);
          break;
        default:
          throw new AssertionError();
      }
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public io.grpc.stub.StreamObserver<Req> invoke(
        io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        default:
          throw new AssertionError();
      }
    }
  }

  private static abstract class ClientServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    ClientServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return org.minbox.framework.message.pipe.core.grpc.ClientServiceProto.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("ClientService");
    }
  }

  private static final class ClientServiceFileDescriptorSupplier
      extends ClientServiceBaseDescriptorSupplier {
    ClientServiceFileDescriptorSupplier() {}
  }

  private static final class ClientServiceMethodDescriptorSupplier
      extends ClientServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    ClientServiceMethodDescriptorSupplier(String methodName) {
      this.methodName = methodName;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.MethodDescriptor getMethodDescriptor() {
      return getServiceDescriptor().findMethodByName(methodName);
    }
  }

  private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    io.grpc.ServiceDescriptor result = serviceDescriptor;
    if (result == null) {
      synchronized (ClientServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new ClientServiceFileDescriptorSupplier())
              .addMethod(getRegisterMethod())
              .addMethod(getHeartbeatMethod())
              .build();
        }
      }
    }
    return result;
  }
}
