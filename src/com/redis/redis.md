### Redis

​	redis 是基于C语言开发的Key-Value存储系统，是跨平台的非关系型数据库。由于其是内存数据库，读写速度非常快，常用来做缓存，也常用来做分布式锁

### 常用数据结构

+ String：Redis自身构建了一种 简单动态字符串(SDS，simple dynamic string)。Redis的SDS可以保存文本数据也可保存二进制数据，且获取字符串长度复杂度为O(1)，且SDS的api是安全的，不会造成缓冲区溢出。

  ```
  set,get,strlen,exists,dect,incr,setex
  ```

+ List：Redis实现的数据结构是双向链表

  ```
  rpush,lpop,lpush,rpop,lrange、llen
  ```

+ Hash：类似JDK1.8以前的HashMap(数组+链表)

  ```
  hset,hmset,hexists,hget,hgetall,hkeys,hvals
  ```

+ Set：类似Java中的HashSet

  ```
  sadd,spop,smembers,sismember,scard,sinterstore,sunion
  ```

+ Sorted Set：和Set相比增加了一个权重参数

  ```
  zadd,zcard,zscore,zrange,zrevrange,zrem
  ```

+ BitMap：存储连续的二进制数字

  ```
  setbit 、getbit 、bitcount、bitop
  ```

### Redis 单线程模型

### 过期数据的删除策略

### Redis内存淘汰机制

### Redis持久化机制

### Redis事务机制

+ MULTI

+ EXEC

+ DISCARD

+ WATCH

  redis不支持回滚操作

### 缓存穿透 缓存雪崩 缓存击穿

