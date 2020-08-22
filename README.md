# Message Pipe
基于`Redis`实现的分布式消息顺序消费管道。

![CI Build](https://github.com/minbox-projects/message-pipe/workflows/CI%20Build/badge.svg)
![](https://img.shields.io/badge/License-Apache%202.0-blue.svg)
![](https://img.shields.io/badge/JDK-1.8+-blue.svg)

## I. 什么是Message Pipe？

`Message Pipe`是基于`Redis`实现的**顺序消息管道**，由于内部引入了`Redisson`分布式锁所以它是线程安全的，多线程情况下也会按照写入管道的顺序执行消费。

`Message Pipe`采用`Client`、`Server`概念进行设计，内部通过`grpc-netty`来建立消息通道相互通信的长连接，消息的分发由`Server`负责，而每一个管道内的消息在分发时会通过`LoadBalance（负载均衡）`的方式来获取在线的`Client`信息并向`Client`顺序发送消息。

## II. 架构图

...

## III. 特性

- 自动注册
- 心跳检查
- 消息分发
- 顺序消费
- 读写分离
- 线程安全
- 负载均衡
- 自动剔除

## IIII. 快速上手

为了快速上手，提供了`message-pipe`使用的示例项目，项目源码：[https://github.com/minbox-projects/message-pipe-example](https://github.com/minbox-projects/message-pipe-example)。

### 4.1 安装Redis

由于`message-pipe`基于`Redis`实现，所以我们首先需要在本机安装`Redis`，下面是使用`Docker`方式安装步骤：

```sh
# 拉取Redis镜像
docker pull redis
# 创建一个名为"redis"的后台运行容器，端口号映射宿主机6379
docker run --name redis -d -p 6379:6379 redis
```

### 4.2 查看Redis数据

```sh
# 运行容器内命令
docker exec -it redis /bin/sh
# 运行Redis客户端
redis-cli
# 选择索引为1的数据库
select 1
# 查看全部的数据
keys *
```

### 4.3 启动示例项目

```sh
# 下载源码
git clone https://github.com/minbox-projects/message-pipe-example.git
# 进入项目目录
cd message-pipe-example
# 运行项目
mvn spring-boot:run
```

## V. License

`message-pipe`采用Apache2开源许可进行编写。

