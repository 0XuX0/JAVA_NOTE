## Mongo

### 什么是Mongo

MongoDB是一个文档数据库，提供好的性能，领先的非关系型数据库。采用BSON存储文档数据。

BSON是一种类JSON的一种二进制形式的存储格式，简称Binary JSON，相比JSON多了date类型和二进制数组。

### MongoDB的优势

+ 面向文档的存储：以JSON格式的文档保存数据
+ 任何属性都可以建立索引
+ 复制以及高可扩展性
+ 自动分片
+ 丰富的查询功能
+ 快速的即时更新

### MongoDB和关系型数据库术语对比图

|  MongoDB   | 关系型数据库 |
| :--------: | :----------: |
|  Database  |   Database   |
| Collection |    Table     |
|  Document  |  Record/Row  |
|   Field    |    Column    |

### 为什么使用MongoDB

+ 架构简单
+ 没有复杂的连接
+ 深度查询能力，MongoDB支持动态查询
+ 容易调试、扩展
+ 无需转化/映射应用对象到数据库对象
+ 使用内部内存作为存储工作区，以便更快的存取数据

### MongoDB中的分片是什么意思

分片是将数据水平切分到不同的物理节点。当数据量增大时，单机有可能无法存储数据或可接收的读写吞吐量。利用分片技术可添加更多的机器来应对数据量增加以及读写操作的要求

### 分析器在MongoDB中的作用是什么

MongoDB中包括了一个可以显示数据库中每个操作性能特点的数据库分析器。通过分析器可找到比预期慢的查询操作。

打开系统分析器 db.setProfilingLevel(2) 收集耗时超过500ms的操作 db.setProfilingLevel(1,500)

### MongoDB中ObjectID有哪些部分组成

时间戳、客户端机器标识、客户进程ID、三个字节的增量计数器

### NoSQL数据库的含义 NoSQL和RDBMS有什么区别？在哪些情况下使用和不使用NoSQL数据库

NoSQL(Not Only SQL)是非关系型数据库。关系型数据库采用结构化数据，NoSQL采用键值对的方式存储数据。在处理非/半结构化的大数据时、水平方向上扩展时、随时应对动态增加的数据项时可优先考虑NoSQL数据库。在考虑数据库的成熟度、支持、分析和商业智能、管理等问题时应优先考虑关系型数据库。

### MongoDB支持存储过程么？

支持，是JavaScript写的，保存在db.system.js中

### 如何理解MongoDB中的GridFS机制

GridFS是一个将大型文件存储在MongoDB中的文件规范。使用GridFS可以将大文件分隔成多个小文档存储，这样可以有效保存大文档，且解决了BSON对象限制的问题

### 更新操作立即fsync到磁盘么

不会，磁盘写操作默认是延迟执行的，写操作可能在两三秒(默认是60秒)后到达磁盘。例如一秒内数据库收到一千个对一个对象递增的操作，仅刷新磁盘一次

### 如何执行事务/加锁

MongoDB没有使用传统的锁或者复杂的带回滚的事务。因为其设计宗旨是轻量、快速以及可预计的高性能。可以类比成mysql的自动提交模式。通过精简对事务的支持，性能得到提升

### NoSQL遵循CAP定理和BASE要求

#### CAP定理

+ 一致性(Consistency) 所有节点在同一时间具有相同的数据
+ 可用性(Availability) 保证每个请求不管成功或者失败都有响应
+ 分区容错性(Partition tolerance) 系统中任意信息的丢失或失败不影响系统的继续运行

CAP理论的核心：一个分布式系统不可能同时很好的满足一致性、可用性和分区容错性这三个要求，最多只能同时较好的满足两个

#### BASE

+ 基本可用(Basically Available)
+ 软状态/柔性事务(Soft-state)
+ 最终一致性(Eventually Consistency)

### MongoDB的索引底层数据结构是什么

WiredTiger引擎中索引的底层是B+树

### 常用Mongo数据库语句

+ 按条件查询最新的十条记录，并以json格式打印

  ``` java
  db.auditLog.find({user:"chenghao", auditOperationType:"DELETE"}).sort({"_id":-1}).limit(10).forEach(printjson)
  ```

+ 索引相关

  ```java
  // 显示当前索引
  db.col.getIndexes()
  // 建立索引
  db.col.createIndex({field1: 1, field2: 2}, {name; "f1_f2_idx"}, {background: true})
  // 删除索引
  db.col.dropIndex("f1_f2_idx")
  // explain
  db.smartEventHit.find({hitState:"NOT_PROCESSED", creationTime: {"$gte":ISODate("2020-11-01T15:40:00.000Z")}, creationTime: {"$lte":ISODate("2020-11-01T15:44:59.999Z")}}).limit(48).sort({"creationTIme": 1}).explain("executionStats")
  ```

+ UPDATTE

  ```java
  db.device.update({},{$set:{"meta.deviceGroupId":"5fb39127130e3875b660cd42"}},{multi:1});
  db.smartEventTask.update( {"_id":ObjectId("5fc8ddf96b769c377458bb63")},{$set:{"algorithmParam.relativePosition.0.deviceId":"e0000110"}} )
  ```

+ DISTINCT

  ```
  db.inventory.distinct(“item.sku”,{dept:”A”})   //满足dept为A数据的item字段的子字段的不重复值
  ```

+ 只展示部分字段结果

  ```
  db.rawTrack.find({"deviceId":"c0000096"},{"subjectId":1,"faceImageQuality":1,"creationTime":1}).sort({"_id":-1}).limit(20)
  ```

+ 查询指定时间范围数据

  ```
  db.CollectionAAA.find({ "CreateTime" : { "$gte" : ISODate("2017-04-20T00:00:00Z"), "$lt" : ISODate("2017-04-21T00:00:00Z") } }).count()
  ```

  