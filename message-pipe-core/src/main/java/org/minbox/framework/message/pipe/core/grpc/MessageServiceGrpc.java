package org.minbox.framework.message.pipe.core.grpc;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.45.1)",
    comments = "Source: MessageService.proto")
@io.grpc.stub.annotations.GrpcGenerated
public final class MessageServiceGrpc {

  private MessageServiceGrpc() {}

  public static final String SERVICE_NAME = "org.minbox.framework.message.pipe.core.grpc.MessageService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<org.minbox.framework.message.pipe.core.grpc.MessageRequest,
      org.minbox.framework.message.pipe.core.grpc.MessageResponse> getMessageProcessingMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "messageProcessing",
      requestType = org.minbox.framework.message.pipe.core.grpc.MessageRequest.class,
      responseType = org.minbox.framework.message.pipe.core.grpc.MessageResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<org.minbox.framework.message.pipe.core.grpc.MessageRequest,
      org.minbox.framework.message.pipe.core.grpc.MessageResponse> getMessageProcessingMethod() {
    io.grpc.MethodDescriptor<org.minbox.framework.message.pipe.core.grpc.MessageRequest, org.minbox.framework.message.pipe.core.grpc.MessageResponse> getMessageProcessingMethod;
    if ((getMessageProcessingMethod = MessageServiceGrpc.getMessageProcessingMethod) == null) {
      synchronized (MessageServiceGrpc.class) {
        if ((getMessageProcessingMethod = MessageServiceGrpc.getMessageProcessingMethod) == null) {
          MessageServiceGrpc.getMessageProcessingMethod = getMessageProcessingMethod =
              io.grpc.MethodDescriptor.<org.minbox.framework.message.pipe.core.grpc.MessageRequest, org.minbox.framework.message.pipe.core.grpc.MessageResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "messageProcessing"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.minbox.framework.message.pipe.core.grpc.MessageRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.minbox.framework.message.pipe.core.grpc.MessageResponse.getDefaultInstance()))
              .setSchemaDescriptor(new MessageServiceMethodDescriptorSupplier("messageProcessing"))
              .build();
        }
      }
    }
    return getMessageProcessingMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static MessageServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<MessageServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<MessageServiceStub>() {
        @java.lang.Override
        public MessageServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new MessageServiceStub(channel, callOptions);
        }
      };
    return MessageServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static MessageServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<MessageServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<MessageServiceBlockingStub>() {
        @java.lang.Override
        public MessageServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new MessageServiceBlockingStub(channel, callOptions);
        }
      };
    return MessageServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static MessageServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<MessageServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<MessageServiceFutureStub>() {
        @java.lang.Override
        public MessageServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new MessageServiceFutureStub(channel, callOptions);
        }
      };
    return MessageServiceFutureStub.newStub(factory, channel);
  }

  /**
   */
  public static abstract class MessageServiceImplBase implements io.grpc.BindableService {

    /**
     */
    public void messageProcessing(org.minbox.framework.message.pipe.core.grpc.MessageRequest request,
        io.grpc.stub.StreamObserver<org.minbox.framework.message.pipe.core.grpc.MessageResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getMessageProcessingMethod(), responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            getMessageProcessingMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                org.minbox.framework.message.pipe.core.grpc.MessageRequest,
                org.minbox.framework.message.pipe.core.grpc.MessageResponse>(
                  this, METHODID_MESSAGE_PROCESSING)))
          .build();
    }
  }

  /**
   */
  public static final class MessageServiceStub extends io.grpc.stub.AbstractAsyncStub<MessageServiceStub> {
    private MessageServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected MessageServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new MessageServiceStub(channel, callOptions);
    }

    /**
     */
    public void messageProcessing(org.minbox.framework.message.pipe.core.grpc.MessageRequest request,
        io.grpc.stub.StreamObserver<org.minbox.framework.message.pipe.core.grpc.MessageResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getMessageProcessingMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   */
  public static final class MessageServiceBlockingStub extends io.grpc.stub.AbstractBlockingStub<MessageServiceBlockingStub> {
    private MessageServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected MessageServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new MessageServiceBlockingStub(channel, callOptions);
    }

    /**
     */
    public org.minbox.framework.message.pipe.core.grpc.MessageResponse messageProcessing(org.minbox.framework.message.pipe.core.grpc.MessageRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getMessageProcessingMethod(), getCallOptions(), request);
    }
  }

  /**
   */
  public static final class MessageServiceFutureStub extends io.grpc.stub.AbstractFutureStub<MessageServiceFutureStub> {
    private MessageServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected MessageServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new MessageServiceFutureStub(channel, callOptions);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<org.minbox.framework.message.pipe.core.grpc.MessageResponse> messageProcessing(
        org.minbox.framework.message.pipe.core.grpc.MessageRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getMessageProcessingMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_MESSAGE_PROCESSING = 0;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final MessageServiceImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(MessageServiceImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_MESSAGE_PROCESSING:
          serviceImpl.messageProcessing((org.minbox.framework.message.pipe.core.grpc.MessageRequest) request,
              (io.grpc.stub.StreamObserver<org.minbox.framework.message.pipe.core.grpc.MessageResponse>) responseObserver);
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

  private static abstract class MessageServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    MessageServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return org.minbox.framework.message.pipe.core.grpc.MessageProto.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("MessageService");
    }
  }

  private static final class MessageServiceFileDescriptorSupplier
      extends MessageServiceBaseDescriptorSupplier {
    MessageServiceFileDescriptorSupplier() {}
  }

  private static final class MessageServiceMethodDescriptorSupplier
      extends MessageServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    MessageServiceMethodDescriptorSupplier(String methodName) {
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
      synchronized (MessageServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new MessageServiceFileDescriptorSupplier())
              .addMethod(getMessageProcessingMethod())
              .build();
        }
      }
    }
    return result;
  }
}
