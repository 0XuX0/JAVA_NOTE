## Kafka

### Kafka是什么？主要应用场景有哪些？

Kafka是一个分布式流式处理平台，具有三个关键功能

1. 消息队列：发布和订阅消息流
2. 容错的持久方式存储记录消息流：Kafka会把消息持久化到磁盘，有效避免了消失丢失的风险
3. 流式处理平台：在消息发布时进行处理，Kafka提供了一个完整的流式处理类库

Kafka主要有两大应用场景：

1. 消息队列：建立实时流数据管道，以可靠地在系统或应用程序之间获取数据
2. 数据处理：构建实时的流数据处理程序来转换或处理数据流

### 和其他消息队列相比，kafka的优势在哪里？

1. 极致的性能：基于Scala和Java语言开发，设计中大量使用了批量处理和异步的思想，最高可每秒处理千万级别的消息
2. 生态系统兼容性无可匹敌

### Kafka的架构图 Producer Consumer Broker Topic Partition

### Kafka的多副本机制了解么，带来了什么好处

生产者和消费者只与leader副本交互。可以理解为其他副本只是leader副本的拷贝，它们的存在只是为了保证消息存储的安全性。当leader副本发生故障时会从follower中选举出一个leader。

#### Kafka的多分区(Partition)以及多副本(Replica)机制的好处有

1. Kafka通过给特定Topic指定多个Partition，而各个Partition可以分布在不同的Broker上，这样能提供比较好的并发能力(负载均衡)
2. Partition可以指定对应的Replica数，这极大地提高了消息存储的安全性，提高了容灾能力

### ZooKeeper 在 Kafka 中的作用了解么

1. Broker注册

   在ZooKeeper上有一个专门进行Broker服务器列表记录的节点。每个Broker在启动时会到ZooKeeper上进行注册，即到/broker/ids下创建属于自己的节点。每个节点就会将自己的IP地址和端口等信息记录到该节点中去

2. Topic注册

   在Kafka中，同一个Topic的消息会被分成多个分区并将其分布在多个Broker上，这些分区信息及与Broker的对应关系也都在ZK上维护。比如创建了一个名为my-topic的主题并且有两个分区，对应ZK上就会有 /brokers/topics/my-topic/partitions/0  /brokers/topics/my-topic/partitions/1

3. 负载均衡

   Kafka通过给特定Topic指定多个Partition，而各个Partition可以分布在不同的Broker上，这样便能提供较好的并发能力。对于同一个Topic的不同Partition，Kafka会尽力将这些Partition分布到不同Broker服务器上。当生产者生产消息后也会尽量投递到不同Broker的Partition里面。当Consumer消费的时候，ZK可以根据当前的Partition数量以及Consumer数量来实现动态负载均衡。

### Kafka如何保证消息的消费顺序

1. 1个Topic只对应一个Partition
2. 发送消息的时候指定 key/Partition

### Kafka如何保证消息不丢失

#### 生产端 Producer

ISR机制：ZK中给每个Partition维护一个ISR列表，包含Leader以及跟Leader保持同步的Follower

1. 必须要求至少一个Follower在ISR列表里
2. 每次写入数据时，要求Leader和至少一个ISR里的Follower写入成功

> request.required.acks = -1 生产者发送消息要等到leader及所有副本都同步了才会返回ack
>
> min.insync.replicas 最小同步副本数量，如果没有达到这个要求，生产者会抛异常

#### 消费端 Consumer

消费者会自动每隔一段时间将offset保存到ZK上，此时如果刚好将偏移量提交到ZK上后，但是数据还没消费完，机器发生宕机，此时数据就丢失了

关闭自动提交，改为手动提交，每次数据处理完后再提交

### Kafka如何保证消息不重复消费



消费者组：由一个或多个消费者实例组成，多个实例共同订阅若干个主题，共同消费。当某个实例挂掉，其他实例会自动承担消费的分区

消费者组的位移提交机制

批量提交 顺序写 零拷贝



### 数据传输的事务定义有哪些

+ 最多一次：消息不会被重复发送，最多被传输一次，但也有可能一次不传输
+ 最少一次：消息不会被漏发送，最少被传输一次，但也有可能被重复传输
+ 精确的一次：不会漏传也不会重复传输，每个消息都传输一次且仅仅被传输一次

### kafka consumer 是否可以消费指定分区的消息

kafka consumer 消费消息时，向broker发出fetch请求去消费特定分区的消息，consumer指定消息在日志中的偏移量(offset)，就可以消费从这个位置开始的消息，customer拥有了offset的控制权，可以向后回滚去重新消费之前的消息

### kafka 的 ack 机制

request.required.acks 有三个值 0 1 -1

+ 0：生产者不会等待 broker 的 ack 这个延迟最低但是存储的保证最弱，当 server 挂掉的时候会丢数据
+ 1：服务端会等待 ack 值，leader 副本确认接收到消息后发送 ack 但是如果 leader 挂掉后他不确保是否复制完成，新的 leader 可能会导致数据丢失
+ -1：在 1 的基础上，服务端会等待所有的 follower 的副本收到数据才会收到 leader 发出的 ack

### kafka 的消费者如何消费数据

消费者每次消费数据的时候，消费者都会记录消费的物理偏移量(offset)的位置，等到下次消费时会接着上次位置继续消费

### kafka 生产数据时数据的分组策略

+ 生产者决定数据产生到集群的哪个 partition 中
+ 每条消息都是以(key value)格式
+ key 是由生产者发送数据传入

### kafka 是如何进行复制的

### kafka 是如何处理来自生产者和消费者请求的

### kafka的存储细节是怎样的



