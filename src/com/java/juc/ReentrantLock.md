## ReentrantLock

###  同步控制抽象基类 Sync

```java
abstract static class Sync extends AbstractQueuedSynchronizer {
    // 锁方法由子类具体实现
    abstract void lock();
    // 非公平尝试获取锁
    final boolean nonfairTryAcquire(int acquires) {
        final Thread current = Thread.currentThread();
        // 获取AQS的state
        int c = getState();
        if (c == 0) {
            // CAS 抢占式获取锁
            if (compareAndSetState(0, acquires)) {
                setExclusiveOwnerThread(current);
                return true;
            }
        }
        // 若当前线程已获得锁，则修改当前线程获取锁的数量
        else if (current == getExclusiveOwnerThread()) {
            int nextc = c + acquires;
            if (nextc < 0)
                throw new Error("Maximum lock count exceeded");
            setState(nextc);
            return true;
        }
        // 非公平尝试获取锁失败
        retrun false;
    }
    // 尝试释放锁
    protected final boolean tryRelease(int releases) {
        int c = getState() - releases;
        // 校验当前线程已获得锁
        if (Thread.currentThread() != getExclusiveOwner())
            throw new IllegalMonitorStateException();
        boolean free = false;
        // 表示锁已全部释放
        if (c == 0) {
            free = true;
            setExclusiveOwnerThread(null);
        }
        setState(c);
        return free;
    }
    
    // 其他方法省略...
}
```

### 非公平锁 同步模块 NonfairSync

```java
static final class NonfairSync extends Sync {
    final void lock() {
        // CAS 尝试获取锁
        if (compareAndSetState(0, 1))
            setExclusiveOwnerThread(Thread.currentThread());
        else
            // 获取锁失败则交给AQS
            acquire(1);
    }
    protected final boolean tryAcquire(int acquires) {
        // 调用 Sync类的方法
        return nonfairTryAcquire(acquires);
    }
}
```

### 公平锁 同步模块 FairSync

```java
static final class FairSync extends Sync {
    final void lock() {
        // 自己不去尝试获取，直接调用AQS方法
        acquire(1);
    }
    
    protected final boolean tryAcquire(int acquires) {
        final Thread current = Thread.currentThread();
        int c = getState();
        if (c == 0) {
            // AQS同步队列中无其他节点后 再进行CAS获取锁
            if (!hasQueuedPredecessors() &&
               compareAndSetState(0,acquires)) {
                setExclusiveOwnerThread(current);
                return true;
            }
        }
        else if (current == getExclusiveOwnerThread()) {
            int nextc = c + acquires;
            if (nextc < 0)
                throw new Error("Maximum lock count exceeded");
            setState(nextc);
            return true;
        }
        return false;
    }
}
```

### 构造函数

```java
public ReentrantLock() { sync = new NonfairSync(); } // 无参构造函数默认是非公平锁
public ReentrantLock(boolean fair) { 
    sync = fair ? new FairSync() : new NonfairSync(); // 入参控制公平/非公平锁
}
```

### 主要方法

| 方法名称                                     | 描述                                                         |
| -------------------------------------------- | ------------------------------------------------------------ |
| void lock()                                  | 若锁处于空闲状态，当前线程获取锁。否则进入AQS同步队列阻塞直至获取锁 |
| void lockInterruptibly()                     | 若当前线程未被中断，则进入AQS同步队列阻塞获取锁，否则抛出异常 |
| boolean tryLock()                            | 若锁可用返回true否则返回false。不会阻塞线程                  |
| boolean tryLock(long timeout, TimeUnit unit) | 若锁在等待时间内没有被其他线程占用，获取该锁                 |
| void unlock()                                | 释放锁                                                       |
| Condition newCondition()                     | 条件对象，获取等待通知组件                                   |
| int getHoldCount()                           | 查询当前线程保持此锁的次数                                   |
| boolean isHeldByCurrentThread()              | 当前线程是否保持锁定                                         |
| boolean isLocked()                           | 此锁是否被任意线程占用                                       |
| boolean isFair()                             | 此锁是否是公平锁                                             |
| Thread getOwner()                            | 返回当前拥有该锁的线程                                       |
| boolean hasQueuedThreads()                   | 返回同步队列中是否有阻塞等待获取该锁的线程                   |
| boolean hasQueuedThread(Thread)              | 返回给定的线程是否处于同步队列中                             |
| int getQueueLength()                         | 返回同步队列中 被阻塞获取该锁的线程数                        |
| Thread getQueuedThreads()                    | 返回同步队列中所有等待的线程                                 |
| boolean hasWaiters(Condition)                | 查询给定的条件是否有线程在等待队列中                         |
| int getWaitQueueLength(Condition)            | 返回Condition的等待队列中 等待被唤醒获取该锁的线程数         |
| Thread getWaitingThreads(Condition)          | 返回Condition的等待队列中所有等待被唤醒获取该锁的线程        |

