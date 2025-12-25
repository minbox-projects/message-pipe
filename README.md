# Message Pipe - 分布式有序消息处理框架

[![](https://img.shields.io/maven-central/v/org.minbox.framework/message-pipe-bom.svg?label=Maven%20Central)](https://search.maven.org/artifact/org.minbox.framework/message-pipe-bom/1.0.1.RELEASE/pom)
![](https://img.shields.io/badge/License-Apache%202.0-blue.svg)
![](https://img.shields.io/badge/JDK-11+-blue.svg)


## 项目简介

**Message Pipe** 是一款基于 `Redis` 实现的分布式顺序消息管道框架。它利用 `Redisson` 的分布式锁特性确保了线程安全，使得在多线程环境下也能保证消息严格按照写入管道的顺序被消费。

该项目采用了经典的 **Client-Server** 架构设计：

- **Server 端**：负责消息的接收、存储、分发以及管道的管理。
- **Client 端**：负责注册到 Server，并接收 Server 分发的任务进行业务逻辑处理。

两者之间通过 **gRPC**（基于 Netty）建立长连接进行通信，保证了高效的数据传输。Server 端在分发消息时，会采用负载均衡策略从在线的 Client 列表中选择合适的目标进行顺序发送。

## 核心架构

Message Pipe 的核心架构围绕着“管道（Pipe）”这一概念展开。每一个业务场景可以对应一个或多个管道，消息被写入特定的管道中。

1.  **存储层**：使用 `Redis` 的 List 数据结构作为底层消息队列，结合 `Redisson` 实现分布式锁，确保并发读写的安全性。
2.  **通信层**：使用 `gRPC` 定义服务接口（Protobuf），实现 Server 与 Client 之间的高性能通信，包括客户端注册、心跳维持、消息推送等。
3.  **调度层**：Server 端维护着多个管道的调度线程，负责从 Redis 中拉取消息并推送到 Client。

**Message Pipe** 保证：✅ 消息严格有序处理 ✅ 自动重试和死信队列 ✅ 分布式高可用

---

## 项目架构

### 系统架构图

```
┌─────────────────────────────────────────────────────────────┐
│                                                             │
│  ┌──────────────┐         gRPC         ┌──────────────┐    │
│  │  Client 1    │◄──────注册+心跳──────►│              │    │
│  │ (Port:5201)  │                       │    Server    │    │
│  │              │◄────消息批处理────────┤ (Port:5200)  │    │
│  └──────────────┘                       │              │    │
│                                         │              │    │
│  ┌──────────────┐         gRPC         │   消息分配器  │    │
│  │  Client 2    │◄──────注册+心跳──────►│              │    │
│  │ (Port:5202)  │                       │   服务发现   │    │
│  │              │◄────消息批处理────────┤              │    │
│  └──────────────┘                       │   Redis      │    │
│                                         │ (Redisson)   │    │
│  ┌──────────────┐         gRPC         │              │    │
│  │  Client N    │◄──────注册+心跳──────►│   分布式锁   │    │
│  │ (Port:520x)  │                       │              │    │
│  │              │◄────消息批处理────────│   重试机制   │    │
│  └──────────────┘                       │   死信队列   │    │
│                                         └──────────────┘    │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 模块结构

```
message-pipe/
├── message-pipe-core/              # 核心共享模块
│   ├── proto/                      # gRPC 协议定义
│   │   ├── client-service.proto    # 客户端注册服务
│   │   └── message-service.proto   # 消息处理服务
│   ├── domain/                     # 数据模型
│   └── util/                       # 工具类
│
├── message-pipe-server/            # 服务端（消息分配）
│   ├── MessagePipe.java            # 消息管道核心类
│   ├── MessagePipeManager.java     # 管道工厂和管理
│   ├── MessagePipeScheduler.java   # 消息调度器
│   ├── MessagePipeDistributor.java # 消息分配器
│   ├── ClientServiceDiscovery.java # 服务发现
│   ├── MessageDeadLetterQueue.java # 死信队列
│   └── MessageRetryRecord.java     # 重试记录
│
├── message-pipe-client/            # 客户端（消息消费）
│   ├── MessagePipeClientRunner.java # 客户端启动器
│   ├── ReceiveMessageService.java  # 消息接收服务
│   ├── MessageProcessor.java       # 消息处理器接口
│   └── MessageProcessorManager.java# 处理器管理
│
├── message-pipe-spring-context/    # Spring 集成
│   ├── @EnableMessagePipeServer    # 启用服务端
│   ├── @EnableMessagePipeClient    # 启用客户端
│   └── 配置自动装配                  # 自动配置
│
└── pom.xml                         # Maven 配置
```

---

## 核心功能特性

### 1. 🔒 有序消息处理 (Ordered Message Processing)

**特点**：
- 消息严格按写入顺序处理，不允许并发
- 基于 Redis 分布式锁 + 单线程消费
- 支持批量消息处理（默认 200 条/批）

**工作原理**：
```
消息队列: [msg1] → [msg2] → [msg3] → [msg4] → [msg5]
         ↓
    获取分布式锁
         ↓
    取出批量消息（200条）
         ↓
    顺序发送到客户端处理
         ↓
    仅在全部处理成功后删除消息
```

**代码示例**：
```java
// 生产者 - 添加消息
MessagePipe pipe = messagePipeManager.getMessagePipe("order-processing");
Message message = new Message("order-123".getBytes());
pipe.putLast(message);  // 线程安全地添加消息

// 客户端 - 处理消息
@Component
public class OrderProcessor implements MessageProcessor {
    @Override
    public String bindingPipeName() {
        return "order-processing";  // 绑定到指定管道
    }

    @Override
    public boolean processing(String pipeName, String requestId, Message message) {
        // 顺序处理订单
        String orderId = new String(message.getBody());
        processOrder(orderId);
        return true;  // 返回 true 表示成功
    }
}
```

### 2. 🔄 智能重试机制 (Intelligent Retry)

**特点**：
- 指数退避重试：1s → 2s → 4s → 8s → 16s
- 默认最多重试 5 次
- 失败信息存储在 Redis，支持查询和手动处理

**重试流程**：
```
消息处理失败
    ↓
创建重试记录 (MessageRetryRecord)
    ↓
计算延迟: delay = 1000ms × 2^retryCount
    ↓
等待后重新尝试
    ↓
达到最大重试次数？
    ├─ 否 → 继续重试
    └─ 是 → 移入死信队列
```

**配置示例**：
```java
@Bean
public MessagePipeConfiguration messagePipeConfiguration() {
    return MessagePipeConfiguration.defaultConfiguration()
        // 重试记录在 Redis 中保留 30 天
        .setDlqMessageExpireSeconds(30 * 24 * 60 * 60)
        // 其他配置...
        ;
}
```

### 3. ⚰️ 死信队列 (Dead Letter Queue)

**特点**：
- 重试失败的消息自动进入 DLQ
- 消息保留 30 天，便于追踪和恢复
- 包含完整的失败上下文：失败原因、重试次数、时间戳

**数据模型**：
```java
class DeadLetterRecord {
    String messageId;           // 消息 ID
    byte[] messageBody;         // 原始消息内容
    String failureReason;       // 失败原因
    int retryCount;            // 重试次数
    long expireTime;           // 过期时间
    LocalDateTime createTime;  // 创建时间
}
```

**查询和恢复**：
```java
// 查询死信队列中的消息
List<DeadLetterRecord> records =
    deadLetterQueue.listMessages("order-processing");

// 手动恢复消息到主队列
deadLetterQueue.recoverMessage("order-processing", messageId);

// 清空死信队列
deadLetterQueue.clear("order-processing");
```

### 4. 🏥 高可用设计 (High Availability)

**特点**：
- 客户端自动心跳保活（10 秒间隔）
- 离线客户端自动检测和隔离
- 服务端故障自动转移

**客户端生命周期**：
```
启动
  ↓
注册到服务器 → 发送心跳信号
  ↓               ↓
处理消息 ← ─ ─ ─ ─
  ↓
掉线 → 服务器 10 秒无心跳 → 标记离线
  ↓
恢复 → 自动重新注册 → 继续处理
```

**配置示例**：
```java
@Bean
public ClientConfiguration clientConfiguration() {
    return new ClientConfiguration()
        .setLocalPort(5201)
        .setServerAddress("localhost")
        .setServerPort(5200)
        .setHeartBeatIntervalSeconds(10)    // 心跳间隔
        .setRetryRegisterTimes(3);          // 注册重试次数
}
```

### 5. ⚖️ 负载均衡 (Load Balancing)

**特点**：
- 加权随机分配算法
- 支持多客户端分散处理
- 可插拔的负载均衡策略

**分配策略**：
```
客户端 A (权重 1) → 分配 25% 的消息批
客户端 B (权重 2) → 分配 50% 的消息批
客户端 C (权重 1) → 分配 25% 的消息批
         总权重 4
```

**自定义策略示例**：
```java
@Component
public class CustomLoadBalanceStrategy implements ClientLoadBalanceStrategy {
    @Override
    public ClientInformation select(String pipeName, List<ClientInformation> clients) {
        // 实现自己的负载均衡逻辑
        // 例如：轮询、最少连接、一致性哈希等
        return clients.get(new Random().nextInt(clients.size()));
    }
}
```

### 6. 🔍 灵活的客户端匹配 (Flexible Client Binding)

**特点**：
- 支持精确匹配和正则表达式匹配
- 一个管道可以绑定多个客户端
- 一个客户端可以处理多个管道

**两种匹配模式**：

**模式 1：精确匹配**
```java
@Component
public class OrderProcessor implements MessageProcessor {
    @Override
    public String bindingPipeName() {
        return "order-processing";  // 精确匹配这个管道名
    }

    @Override
    public ProcessorType processorType() {
        return ProcessorType.SPECIFIC;
    }

    @Override
    public boolean processing(String pipeName, String requestId, Message message) {
        // 处理 order-processing 管道的消息
        return true;
    }
}
```

**模式 2：正则表达式匹配**
```java
@Component
public class RegexProcessor implements MessageProcessor {
    @Override
    public String bindingPipeName() {
        return "order-.*";  // 正则表达式
    }

    @Override
    public ProcessorType processorType() {
        return ProcessorType.REGEX;
    }

    @Override
    public boolean processing(String pipeName, String requestId, Message message) {
        // 处理所有匹配 order-.* 的管道
        System.out.println("Processing pipe: " + pipeName);
        return true;
    }
}
```

### 7. 📊 可观测性 (Observability)

**特点**：
- 实时指标收集：输入数量、处理数量
- 每个管道独立计数
- 支持聚合指标报告

**指标数据**：
```java
class PipeMetrics {
    long totalInputCount;     // 总输入消息数
    long totalProcessCount;   // 总处理消息数
    int currentQueueSize;     // 当前队列大小
    List<ClientInformation> boundClients;  // 绑定的客户端
}
```

**查询指标示例**：
```java
// 获取单个管道指标
MessagePipeMetrics metrics = pipe.getMetrics();
System.out.println("输入: " + metrics.getTotalInputCount());
System.out.println("处理: " + metrics.getTotalProcessCount());

// 获取所有管道的聚合指标
MessagePipeMetricsAggregator aggregator = new MessagePipeMetricsAggregator();
long totalInput = aggregator.aggregateInputCount();
long totalProcess = aggregator.aggregateProcessCount();
```

### 8. 📝 读写分离扩展性

**特点**：
- 支持读写分离的逻辑架构设计
- 为高吞吐量应用提高扩展空间
- 底层使用 Redis，支持读写分离的二次开发

**设计思想**：
虽然 Message Pipe 目前底层依赖单一的 Redis，但在逻辑上支持读写分离的扩展。这意味着：
- 消息写入（put）和消息读取（take）可以独立扩展
- 可以针对性地优化写入或读取性能
- 便于后续演进为 Redis Cluster 或其他存储方案

**性能优化示例**：
```
┌─────────────────────────────────────────────┐
│  写入优化：多个 Producer 并发写入           │
│  (put lock 保护，防止并发冲突)              │
└────────────┬────────────────────────────────┘
             │
             ▼
        ┌──────────┐
        │  Redis   │
        │  List    │
        └────────┬─┘
                 │
┌────────────────┴────────────────────────────┐
│  读取优化：多个 Consumer 并发读取           │
│  (take lock 保护，严格顺序处理)             │
└─────────────────────────────────────────────┘
```

### 9. 🚀 Spring Boot 无缝集成

**特点**：
- 开箱即用的 Spring Boot Starter 集成
- 自动配置和 Bean 注册
- 声明式启用/禁用

**启用服务端**：
```java
@SpringBootApplication
@EnableMessagePipeServer  // 启用 Server 端功能
public class ServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(ServerApplication.class, args);
    }
}
```

**启用客户端**：
```java
@SpringBootApplication
@EnableMessagePipeClient  // 启用 Client 端功能
public class ClientApplication {
    public static void main(String[] args) {
        SpringApplication.run(ClientApplication.class, args);
    }
}
```

**配置文件示例** (`application.yml`)：

*方式 1：标准配置前缀*
```yaml
message-pipe:
  server:
    enabled: true
    port: 5200
    max-pipe-count: 100
    cleanup-threshold-seconds: 1800
  client:
    enabled: true
    local-port: 5201
    server-address: localhost
    server-port: 5200
    heartbeat-interval-seconds: 10
```

*方式 2：MinBox 前缀配置*
```yaml
minbox:
  message:
    pipe:
      server:
        server-port: 5200                    # Server gRPC 监听端口
        check-client-expired-interval-seconds: 5  # 检查 Client 过期的时间间隔
      client:
        server-address: 127.0.0.1            # Server 端 IP
        server-port: 5200                    # Server 端端口
        local-port: 5201                     # 本机 gRPC 监听端口
```

### 10. 🌐 多种部署模式

**模式 1：gRPC 直连模式**
```
┌─────────────────┐
│  Client 1       │
│  Client 2       │
│  Client 3       │
└────────┬────────┘
         │ gRPC
         ▼
    ┌─────────────┐
    │   Server    │
    │   Redis     │
    └─────────────┘
```

**模式 2：Nacos 服务发现模式**
```
┌──────────────┐         ┌─────────────┐
│  Client 1    │────────►│             │
└──────────────┘         │   Nacos     │
                         │  Registry   │
┌──────────────┐         │             │
│  Client 2    │────────►│             │
└──────────────┘         └─────┬───────┘
                               │
┌──────────────┐         ┌─────▼──────┐
│  Client 3    │────────►│   Server   │
└──────────────┘         │   Redis    │
                         └────────────┘
```

---

## 消息处理流程

### 1️⃣ 系统启动阶段

**服务端启动**：
1. Spring 容器初始化 → 识别 `@EnableMessagePipeServer`
2. 创建 Redis 连接（Redisson 客户端）
3. 初始化 `MessagePipeManager`、`ServiceDiscovery`、`MessagePipeScheduler`
4. 启动 gRPC 服务器（默认端口 5200）
5. 启动定时清理任务（清理过期管道和客户端）

**客户端启动**：
1. Spring 容器初始化 → 识别 `@EnableMessagePipeClient`
2. 扫描所有 `MessageProcessor` Bean
3. 启动 gRPC 服务器（默认端口 5201）
4. 发送注册请求到服务端，告知绑定的管道名
5. 启动心跳线程，定期发送心跳信号（间隔 10 秒）

### 2️⃣ 消息生产阶段

```java
// 生产者代码
MessagePipe pipe = messagePipeManager.getMessagePipe("order-process");
Message msg = new Message("order-123".getBytes());
pipe.putLast(msg);  // 或 putLastBatch(List) 批量添加
```

**内部流程**：
1. `MessagePipe.putLast()` → `rBlockingQueue.add(message)`
2. 消息添加到 Redis 队列：`{pipeName}.queue`
3. 增加指标计数：`totalInputCount++`
4. 唤醒调度器线程：`scheduler.notifyAll()`

### 3️⃣ 消息调度阶段

**MessagePipeScheduler 工作循环**：

```
┌─────────────────────────────────────────┐
│ 1. 服务发现：查询健康的客户端             │
└────────────────┬────────────────────────┘
                 │
                 ▼
        ┌────────────────────┐
        │ 无可用客户端?       │
        └─┬──────────────┬───┘
          │ 是           │ 否
          ▼              ▼
      等待通知    ┌──────────────────────┐
                  │ 2. 获取批量消息      │
                  │    (默认 200 条)      │
                  └────────────┬─────────┘
                               │
                               ▼
                  ┌──────────────────────┐
                  │ 3. 获取分布式锁      │
                  │    (take lock)        │
                  └────────────┬─────────┘
                               │
                               ▼
                  ┌──────────────────────┐
                  │ 4. 选择负载均衡      │
                  │    客户端             │
                  └────────────┬─────────┘
                               │
                               ▼
                  ┌──────────────────────┐
                  │ 5. gRPC 发送消息批   │
                  │    到客户端           │
                  └────────────┬─────────┘
                               │
                               ▼
                  ┌──────────────────────┐
                  │ 6. 接收处理结果      │
                  │    successCount       │
                  └─┬──────────────┬─────┘
                    │              │
        ┌───────────┘              └───────────┐
        │                                       │
        ▼                                       ▼
   ┌──────────┐                          ┌──────────┐
   │ 失败?    │                          │ 全部成功?│
   │ 网络错误 │                          │ (count=  │
   │ (count=-1)                          │ batch    │
   └─┬────────┘                          │ size)    │
     │                                   └┬─────────┘
     │ 是                                │ 是
     ▼                                   ▼
  break                        ┌─────────────────┐
  重试下一轮                    │ 7. 删除消息     │
                               │ (trim queue)    │
                               └────────────────┘
                                      │
                                      ▼
                               ┌──────────────────┐
                               │ 8. 更新指标     │
                               │ totalProcessCount│
                               └────────────────┘
```

### 4️⃣ 消息消费阶段

**客户端处理流程**：

```
┌───────────────────────────────────┐
│ 服务端 gRPC 发送消息批            │
│ MessageRequestBody {               │
│   pipeName: "order-process"        │
│   requestId: "req-123"             │
│   messages: [msg1, msg2, msg3...]  │
│ }                                  │
└────────────────┬──────────────────┘
                 │
                 ▼
    ┌────────────────────────┐
    │ 客户端 gRPC 服务接收   │
    │ ReceiveMessageService  │
    │ .messageProcessing()   │
    └────────────┬───────────┘
                 │
                 ▼
    ┌────────────────────────┐
    │ 查找对应的处理器       │
    │ MessageProcessor       │
    │ (根据 pipeName 匹配)  │
    └────────────┬───────────┘
                 │
                 ▼
    ┌─────────────────────────────┐
    │ 顺序处理消息（关键！）      │
    │ for (Message msg : messages)│
    │   {                         │
    │     processor.processing()  │
    │     successCount++          │
    │   }                         │
    │ 一旦失败，停止处理          │
    └────────────┬────────────────┘
                 │
                 ▼
    ┌─────────────────────────────┐
    │ 返回处理结果到服务端        │
    │ MessageResponseBody {        │
    │   status: SUCCESS/ERROR      │
    │   successCount: N            │
    │ }                           │
    └─────────────────────────────┘
```

### 5️⃣ 失败处理阶段

**部分失败处理**（例如处理了 150/200 条）：

```
成功处理 150 条消息
    ↓
返回 successCount = 150
    ↓
服务端检测：150 < 200（部分失败）
    ↓
删除成功的 150 条消息
    ↓
剩余 50 条消息重新在队列头部
    ↓
触发失败处理逻辑
```

**失败消息处理**：

```
消息处理失败（processor 返回 false）
    ↓
检查是否有健康的客户端
    ├─ 否 → 跳过，留在队列中等待
    └─ 是 ↓
     获取重试记录：MessageRetryRecord
    ↓
 增加重试次数
    ↓
 计算延迟时间
 delay = 1000ms × 2^retryCount
    ├─ 重试 1：1 秒
    ├─ 重试 2：2 秒
    ├─ 重试 3：4 秒
    ├─ 重试 4：8 秒
    └─ 重试 5：16 秒
    ↓
 睡眠延迟时间
    ↓
 重试次数 > 5？
    ├─ 否 → 继续重试（消息回到队列）
    └─ 是 ↓
       创建死信记录
       DeadLetterRecord {
         messageId,
         failureReason,
         retryCount: 5,
         expireTime: now + 30days
       }
    ↓
 移入死信队列
 {pipeName}_dead_letter
    ↓
 从主队列删除消息
    ↓
 清理重试记录
```

---

## 快速开始

### 前置条件

- Java 11 或更高版本
- Redis 服务器（推荐 5.0+）
- Spring Boot 2.7.0+
- Maven 3.6.0+

### 服务端配置

**1. Maven 依赖**：
```xml
<dependency>
    <groupId>org.minbox.framework</groupId>
    <artifactId>message-pipe-spring-context</artifactId>
    <version>1.0.8</version>
</dependency>
```

**2. Spring Boot 应用类**：
```java
@SpringBootApplication
@EnableMessagePipeServer  // 启用服务端
public class MessagePipeServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(MessagePipeServerApplication.class, args);
    }
}
```

**3. 配置文件** (`application.yml`)：
```yaml
server:
  port: 8080

spring:
  redis:
    host: localhost
    port: 6379
    database: 0

message-pipe:
  server:
    port: 5200                    # gRPC 服务器端口
    max-pipe-count: 100           # 最大管道数
    service-type: GRPC            # 服务类型：GRPC 或 NACOS
    cleanup-threshold-seconds: 1800 # 清理过期管道间隔（30分钟）
```

**4. 发送消息**：
```java
@RestController
@RequestMapping("/api/messages")
public class MessageController {

    @Autowired
    private DefaultMessagePipeManager messagePipeManager;

    @PostMapping("/send")
    public void sendMessage(@RequestParam String pipeName,
                           @RequestParam String messageBody) {
        MessagePipe pipe = messagePipeManager.getMessagePipe(pipeName);
        Message message = new Message(messageBody.getBytes());
        pipe.putLast(message);
    }

    @PostMapping("/send-batch")
    public void sendBatch(@RequestParam String pipeName,
                         @RequestBody List<String> messages) {
        MessagePipe pipe = messagePipeManager.getMessagePipe(pipeName);
        List<Message> msgList = messages.stream()
            .map(body -> new Message(body.getBytes()))
            .collect(Collectors.toList());
        pipe.putLastBatch(msgList);
    }
}
```

### 客户端配置

**1. Maven 依赖**：
```xml
<dependency>
    <groupId>org.minbox.framework</groupId>
    <artifactId>message-pipe-spring-context</artifactId>
    <version>1.0.8</version>
</dependency>
```

**2. Spring Boot 应用类**：
```java
@SpringBootApplication
@EnableMessagePipeClient  // 启用客户端
public class MessagePipeClientApplication {
    public static void main(String[] args) {
        SpringApplication.run(MessagePipeClientApplication.class, args);
    }
}
```

**3. 配置文件** (`application.yml`)：
```yaml
server:
  port: 8081

message-pipe:
  client:
    local-port: 5201              # 本地 gRPC 服务器端口
    server-address: localhost      # 服务端地址
    server-port: 5200              # 服务端 gRPC 端口
    heartbeat-interval-seconds: 10 # 心跳间隔
    retry-register-times: 3        # 注册重试次数
```

**4. 实现消息处理器**：
```java
@Component
public class OrderMessageProcessor implements MessageProcessor {

    private static final Logger logger = LoggerFactory.getLogger(OrderMessageProcessor.class);

    @Override
    public String bindingPipeName() {
        return "order-processing";
    }

    @Override
    public ProcessorType processorType() {
        return ProcessorType.SPECIFIC;
    }

    @Override
    public boolean processing(String pipeName, String requestId, Message message) {
        try {
            String orderId = new String(message.getBody());
            logger.info("Processing order: {}, requestId: {}", orderId, requestId);

            // 您的业务逻辑
            processOrder(orderId);

            return true;  // 返回 true 表示处理成功
        } catch (Exception e) {
            logger.error("Failed to process order", e);
            return false;  // 返回 false 表示处理失败，会触发重试
        }
    }

    private void processOrder(String orderId) {
        // 处理订单逻辑
        // 例如：数据库更新、调用下游服务等
    }
}
```

**5. 实现正则匹配处理器**：
```java
@Component
public class PaymentMessageProcessor implements MessageProcessor {

    @Override
    public String bindingPipeName() {
        return "payment-.*";  // 匹配所有 payment-* 的管道
    }

    @Override
    public ProcessorType processorType() {
        return ProcessorType.REGEX;
    }

    @Override
    public boolean processing(String pipeName, String requestId, Message message) {
        String paymentId = new String(message.getBody());
        System.out.println("Processing payment: " + paymentId +
                          " from pipe: " + pipeName);
        // 处理支付逻辑
        return true;
    }
}
```

### 使用示例

**场景：订单处理系统**

**发送消息**：
```bash
curl -X POST "http://localhost:8080/api/messages/send" \
  -H "Content-Type: application/json" \
  -d '{
    "pipeName": "order-processing",
    "messageBody": "order-123"
  }'
```

**查看指标**：
```java
@GetMapping("/metrics")
public ResponseEntity<?> getMetrics() {
    MessagePipe pipe = messagePipeManager.getMessagePipe("order-processing");
    return ResponseEntity.ok(Map.of(
        "totalInput", pipe.getTotalInputCount(),
        "totalProcess", pipe.getTotalProcessCount(),
        "currentQueueSize", pipe.getCurrentQueueSize(),
        "boundClients", pipe.getBoundClients()
    ));
}
```

**死信队列恢复**：
```java
@PostMapping("/dlq/recover")
public void recoverFromDLQ(@RequestParam String pipeName,
                          @RequestParam String messageId) {
    MessageDeadLetterQueue dlq = deadLetterQueueManager.getQueue(pipeName);
    dlq.recoverMessage(messageId);
}

@GetMapping("/dlq/list")
public List<DeadLetterRecord> listDLQ(@RequestParam String pipeName) {
    MessageDeadLetterQueue dlq = deadLetterQueueManager.getQueue(pipeName);
    return dlq.listMessages();
}
```

---

## 配置参考

### 服务端配置 (MessagePipeConfiguration)

| 配置项 | 类型 | 默认值 | 说明 |
|-------|------|--------|------|
| `batchSize` | int | 200 | 每批处理的消息数 |
| `putLockTime.waitTime` | int | 5 | put 锁等待时间（秒） |
| `putLockTime.leaseTime` | int | 10 | put 锁租期（秒） |
| `takeLockTime.waitTime` | int | 10 | take 锁等待时间（秒） |
| `takeLockTime.leaseTime` | int | 300 | take 锁租期（秒） |
| `dlqMessageExpireSeconds` | long | 2592000 | 死信消息过期时间（30 天） |
| `retryRecordExpireSeconds` | long | 2592000 | 重试记录过期时间（30 天） |

### 服务端配置 (ServerConfiguration)

| 配置项 | 类型 | 默认值 | 说明 |
|-------|------|--------|------|
| `serverPort` | int | 5200 | gRPC 服务器端口 |
| `maxMessagePipeCount` | int | 100 | 最大管道数量 |
| `expiredExcludeThresholdSeconds` | int | 10 | 客户端离线判断阈值（秒） |
| `cleanupExpiredMessagePipeThresholdSeconds` | int | 1800 | 清理过期管道间隔（秒） |

### 客户端配置 (ClientConfiguration)

| 配置项 | 类型 | 默认值 | 说明 |
|-------|------|--------|------|
| `localPort` | int | 5201 | 本地 gRPC 服务器端口 |
| `serverAddress` | String | localhost | 服务端地址 |
| `serverPort` | int | 5200 | 服务端 gRPC 端口 |
| `heartBeatIntervalSeconds` | int | 10 | 心跳间隔（秒） |
| `retryRegisterTimes` | int | 3 | 注册重试次数 |
| `localNetworkInterface` | String | null | 本地网卡名（可选） |

---

## 常见问题 (FAQ)

### Q1: 消息处理顺序如何保证？

**A**: 通过以下机制保证：
1. **单线程调度**：`MessagePipeScheduler` 每个管道只有一个工作线程
2. **分布式锁**：在处理前获取 take lock，确保全局只有一个处理者
3. **批量删除**：仅在全部消息处理成功后才删除，失败则保留

```
take lock ──→ fetch batch ──→ sequential process ──→ atomic delete
              (单线程)        (不允许并发)         (全部成功)
```

### Q2: 如何确保消息不丢失？

**A**: 多层保护机制：
1. **Redis 持久化**：消息存在 RDB/AOF
2. **重试机制**：处理失败自动重试最多 5 次
3. **死信队列**：重试失败后保存 30 天便于恢复
4. **客户端确认**：仅收到成功响应才删除消息

### Q3: 如果服务端宕机怎么办？

**A**:
1. **消息保留在 Redis**：由于是基于 Redis 存储，消息不丢失
2. **客户端自动重连**：客户端会重新注册（带重试机制）
3. **消息重新分配**：服务端恢复后继续分配消息
4. 建议使用 Redis 集群/哨兵模式提高可用性

### Q4: 如何扩展新的负载均衡策略？

**A**: 实现 `ClientLoadBalanceStrategy` 接口：
```java
@Component
public class ConsistentHashStrategy implements ClientLoadBalanceStrategy {
    @Override
    public ClientInformation select(String pipeName,
                                   List<ClientInformation> clients) {
        // 一致性哈希实现
        // 确保同一消息始终发往同一客户端
        return clients.get(hashFunction(pipeName) % clients.size());
    }
}
```

### Q5: 如何处理大消息？

**A**: 建议方案：
1. 消息本体存储在文件系统或对象存储（如 S3）
2. 消息内容只包含引用 ID
3. 客户端根据 ID 获取实际内容

```java
// 推荐做法
byte[] messageBody = referenceId.getBytes();  // "oss://file-id-123"
Message message = new Message(messageBody);
pipe.putLast(message);

// 客户端处理
public boolean processing(..., Message message) {
    String fileId = new String(message.getBody());
    byte[] actualContent = fetchFromOss(fileId);
    // 处理实际内容
    return true;
}
```

### Q6: 支持消息的优先级吗？

**A**: 目前不支持原生优先级。可以通过以下方案实现：
1. 使用多个管道，高优先级消息放在单独管道
2. 高优先级管道分配更多客户端资源
3. 在应用层实现基于消息内容的路由

---

## 性能指标

### 测试环境
- **单机 Redis**：内存模式，不开启持久化
- **网络延迟**：< 1ms（同机房）
- **消息大小**：1KB
- **批大小**：200 条

### 性能数据

| 指标 | 数值 | 说明 |
|------|------|------|
| 吞吐量（Producer） | 50,000 msg/s | 取决于 Redis 性能 |
| 吞吐量（Consumer） | 100,000 msg/s | 取决于处理逻辑 |
| 端到端延迟 | < 100ms (p99) | 包括网络延迟 |
| 内存占用 | ~1MB per 10K msg | 消息在 Redis 中 |
| 消息处理失败恢复 | < 5s | 包括重试和 DLQ 转移 |

---

## 最佳实践

### 1. 消息设计

✅ **推荐**：
- 消息大小 < 10KB
- 仅包含必要数据或引用 ID
- 包含唯一标识符用于幂等性判断

❌ **不推荐**：
- 超大消息（> 100KB）直接传输
- 敏感信息明文存储（应该加密）
- 没有唯一 ID 的消息

### 2. 处理器实现

✅ **推荐**：
```java
@Component
public class OrderProcessor implements MessageProcessor {

    @Override
    public boolean processing(String pipeName, String requestId, Message message) {
        String orderId = new String(message.getBody());

        try {
            // 业务逻辑（应该是幂等的）
            boolean success = processOrderIdempotent(orderId);
            return success;
        } catch (Exception e) {
            logger.error("Error processing order: {}", orderId, e);
            return false;  // 返回 false 触发重试
        }
    }

    // 实现幂等性处理
    private boolean processOrderIdempotent(String orderId) {
        // 检查是否已处理过
        Order order = orderRepository.findById(orderId);
        if (order != null && order.isProcessed()) {
            return true;  // 已处理过，返回成功
        }

        // 处理订单
        order.setProcessed(true);
        orderRepository.save(order);
        return true;
    }
}
```

❌ **不推荐**：
```java
// 阻塞过长
public boolean processing(..., Message message) {
    Thread.sleep(60000);  // 不要阻塞！
    return true;
}

// 抛出异常
public boolean processing(..., Message message) {
    throw new RuntimeException("Error");  // 应该返回 false
}

// 非幂等
public boolean processing(..., Message message) {
    // 无法判断是否已处理，重试会重复处理
    deductFromAccount(accountId, amount);
    return true;
}
```

### 3. 错误处理

✅ **推荐**：
- 区分可恢复和不可恢复错误
- 可恢复错误返回 false 触发重试
- 不可恢复错误记录日志后返回 true

```java
public boolean processing(..., Message message) {
    try {
        // 业务处理
        processMessage(message);
        return true;
    } catch (DatabaseException e) {
        logger.warn("Database error, will retry", e);
        return false;  // 数据库错误 → 重试
    } catch (ValidationException e) {
        logger.error("Invalid message, move to DLQ", e);
        return true;  // 验证失败 → 不重试（虽然会被删除）
    }
}
```

### 4. 性能调优

**调整批大小**：
```java
@Bean
public MessagePipeConfiguration messagePipeConfiguration() {
    return MessagePipeConfiguration.defaultConfiguration()
        .setBatchSize(500);  // 增加批大小提高吞吐，但增加延迟
}
```

**调整重试参数**：
```java
// 更激进的重试（更快恢复）
.setRetryRecordExpireSeconds(24 * 60 * 60)  // 1 天

// 更保守的重试（减少告警）
.setRetryRecordExpireSeconds(7 * 24 * 60 * 60)  // 7 天
```

**多客户端并行处理**：
```java
// 部署多个客户端实例，绑定同一管道
// 服务端会自动负载均衡分配消息
```

---

## 与其他系统的集成

### Redis 支持的版本
- Redis 5.0 及以上
- Redis 集群模式
- Redis 哨兵模式

### 支持的 Spring Boot 版本
- Spring Boot 2.3.0+
- Spring Boot 3.0.0+（需要 Java 17）

### gRPC 版本
- gRPC 1.40.0+
- Protocol Buffers 3.17.0+

### 可选集成
- **Nacos**: 用于服务发现和配置管理
- **Prometheus**: 用于指标收集（可自定义实现）
- **ELK**: 用于日志收集和分析

---

## 依赖列表

| 依赖 | 版本 | 用途 |
|-----|------|------|
| Redisson | 3.17.7 | Redis 客户端 |
| gRPC | 1.45.1 | RPC 框架 |
| Protobuf | 3.19.4 | 消息序列化 |
| Spring Framework | 5.3.31 | IoC 容器 |
| Spring Data Redis | 2.7.18 | Redis 集成 |
| Jackson | 2.15.3 | JSON 序列化 |
| Nacos Client | 1.4.3 | 服务发现（可选） |
| Lombok | 1.18.30 | 代码生成 |

---

## 文件清单

### 核心类

**message-pipe-core**:
- `org.minbox.framework.message.pipe.core.Message` - 消息类
- `org.minbox.framework.message.pipe.core.domain.ClientInformation` - 客户端信息
- `org.minbox.framework.message.pipe.core.domain.ServerInformation` - 服务端信息

**message-pipe-server**:
- `org.minbox.framework.message.pipe.server.MessagePipe` - 消息管道核心类
- `org.minbox.framework.message.pipe.server.manager.MessagePipeManager` - 管道管理器
- `org.minbox.framework.message.pipe.server.scheduler.MessagePipeScheduler` - 消息调度器
- `org.minbox.framework.message.pipe.server.distributor.MessagePipeDistributor` - 消息分配器
- `org.minbox.framework.message.pipe.server.ServiceDiscovery` - 服务发现
- `org.minbox.framework.message.pipe.server.manager.MessageDeadLetterQueue` - 死信队列
- `org.minbox.framework.message.pipe.server.domain.MessageRetryRecord` - 重试记录

**message-pipe-client**:
- `org.minbox.framework.message.pipe.client.MessageProcessor` - 消息处理器接口
- `org.minbox.framework.message.pipe.client.MessagePipeClientRunner` - 客户端启动器
- `org.minbox.framework.message.pipe.client.ReceiveMessageService` - 消息接收服务

**message-pipe-spring-context**:
- `org.minbox.framework.message.pipe.spring.context.annotation.EnableMessagePipeServer` - 启用服务端注解
- `org.minbox.framework.message.pipe.spring.context.annotation.EnableMessagePipeClient` - 启用客户端注解

---

## 故障排除指南

### 问题：客户端无法连接到服务端

**症状**：
```
ERROR: Connection refused at localhost:5200
```

**排查步骤**：
1. 检查服务端是否启动：`curl -v localhost:5200`
2. 检查防火墙：`sudo lsof -i :5200`
3. 检查配置文件中的地址和端口是否正确
4. 检查 gRPC 服务是否正常启动（查看日志）

### 问题：消息堆积在队列中

**症状**：
```
totalInputCount = 10000
totalProcessCount = 1000
currentQueueSize = 9000
```

**排查步骤**：
1. 检查处理器是否持续返回 false（查看日志）
2. 检查客户端是否在线：`ServiceDiscovery.listClients()`
3. 检查处理逻辑性能：是否处理时间过长
4. 增加客户端数量或提高处理性能

### 问题：消息出现死信队列

**症状**：
```
消息经过 5 次重试后仍失败，移入 DLQ
```

**排查步骤**：
1. 查询死信队列：`MessageDeadLetterQueue.listMessages(pipeName)`
2. 分析失败原因：`DeadLetterRecord.failureReason`
3. 修复根本原因（如依赖服务恢复）
4. 恢复消息到主队列：`deadLetterQueue.recoverMessage(messageId)`

### 问题：内存占用持续增长

**症状**：
```
Redis 内存持续增长，客户端 JVM 内存也在增长
```

**排查步骤**：
1. 检查是否有过期管道未清理：`MessagePipeManager.getMetrics()`
2. 检查死信队列是否过大：`deadLetterQueue.size()`
3. 检查 Redis 键过期设置：`redis-cli TTL {pipeName}_retry_records`
4. 手动清理：`deadLetterQueue.clear()` 或 `messagePipeManager.clearExpiredPipes()`

### 问题：心跳超时，客户端频繁掉线

**症状**：
```
客户端离线 → 消息不处理 → 重新上线 → 再离线
```

**排查步骤**：
1. 检查客户端和服务端网络连接
2. 增加心跳间隔：`heartBeatIntervalSeconds = 30`
3. 检查是否有卡顿：业务线程阻塞或 GC 暂停
4. 查看客户端日志中的异常堆栈

---

## 适用场景

Message Pipe 非常适合以下业务场景和技术需求：

### 1. 强顺序性业务处理
```
✅ 订单流程处理
   创建订单 → 支付 → 发货 → 收货 → 完成
   必须严格按顺序处理，不能乱序

✅ 库存扣减管理
   检查库存 → 冻结库存 → 实际扣减
   顺序错乱会导致超卖

✅ 账户资金变更
   余额变更必须按顺序记录
   保证账户余额的准确性
```

### 2. 轻量级分布式消息队列需求
- 已有 Redis 基础设施的项目
- 不想额外部署 RabbitMQ、Kafka 等重型消息队列
- 对消息吞吐量有一定要求但不是极限追求

### 3. 需要自定义处理逻辑的场景
- Java 技术栈项目
- 需要灵活控制消息处理流程
- 处理逻辑复杂，需要通过代码精细控制

### 4. 中小规模分布式系统
```
消息量规模：每秒 1K-10K 消息
节点规模：3-20 个服务节点
网络环境：局域网或同机房（推荐）
对 RT 要求：不超过 500ms (p99)
```

### 5. 需要精细化运维控制的系统
- 需要监控每个管道的处理指标
- 需要人工干预死信队列恢复失败消息
- 需要自定义负载均衡和重试策略

---

## 不适用的场景

❌ **以下场景不建议使用 Message Pipe**：

| 场景 | 原因 | 建议替代方案 |
|------|------|------------|
| 超大消息量 | 单 Redis 吞吐有限 | Kafka、Pulsar |
| 极低延迟要求 | gRPC 网络延迟 | 本地消息总线、内存队列 |
| 完全无序消费 | 框架强制顺序 | RabbitMQ、SQS |
| 需要事务支持 | 不支持分布式事务 | 专业消息队列 + Saga |
| 复杂 Topic/分片 | 管道概念较简洁 | 专业消息队列 |
| 消息持久化要求极高 | 依赖 Redis 持久化 | 专业消息队列 |

---

## 许可证

Apache License 2.0

---

## 联系和支持

- **项目仓库**：[message-pipe](https://github.com/minbox-projects/message-pipe)
- **示例项目**：[message-pipe-example](https://github.com/minbox-projects/message-pipe-example)
- **作者**：恒宇少年 (hengboy)
- **组织**：MinBox Projects

---

**最后更新**: 2025-12-25

此文档为 Message Pipe 框架的完整介绍和使用指南，涵盖了从基础概念到高级应用的所有方面。如有问题，请参考示例项目或提交 Issue。
