## AQS

  AQS，是指 AbstractQueuedSynchronizer，它提供了一种实现阻塞锁和一系列依赖FIFO等待队列的同步器的框架，常见的如 ReentrantLock、Semaphore、CountDowntLatch、CyclicBarrier等并发类均是基于AQS实现。

### 共享资源变量 State

```java
// 0代表锁未被占用，1代表锁已被占用
private volatile int state;

// 有三种访问方式
// 具有内存读可见性语义
protected final int getState() {
    return state;
}
// 具有内存写可见性语义
protected final void setState(int newState) {
    state = newState;
}
/*
 * 原子地更新同步状态的值(CAS) 具有内存读/写可见性语义
 */
protected final boolean compareAndSetState(int expect, int update) {
    return unsafe.compareAndSwapInt(this, stateOffset, expect, update);
}
```

### CLH 队列（FIFO）

通过内部类 Node 实现 FIFO 队列

```java
//      +------+  prev +-----+       +-----+
// head |      | <---- |     | <---- |     |  tail
//      +------+       +-----+       +-----+
static final class Node {
    // 共享模式节点
    static final Node SHARED = new Node();
    // 独占模式节点
    static final Node EXCLUSIVE = null;
    
    // 线程超时或被中断时进入取消状态
    static final int CANCELLED = 1;
    // 当前节点被释放或取消时，提前通知后继节点对应的线程需要被唤醒
    static final int SIGNAL = -1;
    // 当前节点处于condition队列中，被阻塞
    static final int CONDITION = -2;
    // 传播-适用于共享模式共享节点释放时需传播给其他节点
    static final int PROPAGATE = -3;
    
    // 可选有 -3 -2 -1 0 1
    volatile int waitStatus;
    volatile Node prev;
    volatile Node next;
    volatile Thread thread;
    // Condition队列使用，存储condition队列中的后继节点
    Node nextWaiter;
}

// 指向同步队列队头
private transient volatile Node head;
// 指向同步队列队尾
private transient volatile Node tail;
```

### 获取资源（独占）

```java
public final void acquire(int arg) {
    if (!tryAcquire(arg) &&
        acquireQueued(addWaiter(Node.EXCLUSIVE), arg))
        selfInterrupt();
}

// 尝试以独占的模式获取资源，具体实现由扩展了AQS的同步器完成
protected boolean tryAcquire(int arg) {
    throw new UnsupportedOperationException();
}

/*
* 为当前线程以指定模式创建节点，并添加到等待队列尾部
*/
private Node addWaiter(Node mode) {
    Node node = new Node(Thread.currentThread(), mode);
    // 尝试将节点快速插入等待队列，若失败则执行end方法插入
    Node pred = tail;
    if (pred != null) {
        node.prev = pred;
        if (compareAndSetTail(pred, node)) {
            pred.next = node;
            return node;
        }
    }
    enq(node);
    return node;
}

/*
* 插入队列
*/
private Node enq(final Node node) {
    // 自旋过程
    for (;;) {
        Node t = tail;
        if (t == null) { // 等待队列为空需进行初始化
            if (compareAndSetHead(new Node()))
                tail = head;
        } else {
            node.prev = t;
            // CAS 将node置为队尾
            if (compareAndSetTail(t, node)) {
                t.next = node;
                return t;
            }
        }
    }
}

final boolean acquireQueued(final Node node, int arg) {
    boolean failed = true; // 标识是否获取资源失败
    try {
        boolean interrupted = false; // 标识当前线程是否被中断过
        // 自旋操作
        for (;;) {
            final Node p = node.predecessor(); //获取当前节点的前继节点
            // 如果前继节点为头节点，说明即将轮到当前节点，可以尝试获取资源
            if (p == head && tryAcquire(arg)) {
                // 将当前节点置为头节点
                setHead(node);
                p.next = null; // help GC
                // 标识获取资源成功
                failed = false;
                // 返回中断标记
                return interrupted;
            }
            // 如果前继节点不是头节点或者获取资源失败，
            // 调用 shouldParkAfterFailedAcquire 函数 判断是否需要阻塞当前节点持有的线程
            // 若需要阻塞，调用 parkAndCheckInterrupt 函数 检查是否可以被中断
            if (shouldParkAfterFailedAcquire(p, node) &&
                parkAndCheckInterrupt())
                interrupted = true;
        }
    } finally {
        // 最终获取资源失败，当前节点放弃获取资源
        if (failed)// 只有node.predecessor()或tryAcquire()方法抛异常时 failed才可能为true
            cancelAcquire(node);
    }
}

// 通过前继节点的waitStatus值来判断是否阻塞当前节点的线程
private static boolean shouldParkAfterFailedAcquire(Node pred, Node node) {
    int ws = pred.waitStatus;
    // 若前继节点完成资源的释放或者中断后，会通知当前节点
    if (ws == Node.SIGNAL)
        return true;
    // 若前继节点处于Cancelled状态，则往前遍历直至最新的前继节点的ws值为0或负数
    if (ws > 0) {
        do {
            node.prev = pred = pred.prev;
        } while (pred.waitStatus > 0);
        pred.next = node;
    }
    // 若前继节点的ws为 -2 -3 则将其值设置为Node.SIGNAL，以保证下次自旋时，shouldParkAfterFailedAcquire 返回true
    else {
        compareAndSetWaitStatus(pred, ws, Node.SIGNAL);
    }
    return false;
}

private final boolean parkAndCheckInterrupt() {
    // 阻塞当前线程
    LockSupport.park(this);
    // 返回线程是否被中断过
    return Thread.interrupted();
}

// 总结
// 1.通过tryAcquire(arg)尝试获取锁资源，若获取成功则直接返回，若获取失败则将该线程以独占模式添加到等待队列尾部
// 2.当线程加入等待队列后，通过acquireQueued方法基于CAS自旋不断尝试获取资源，直至获取到资源
// 3.若获取失败，根据前继节点的状态判断是否可以阻塞，若可以阻塞则调用LockSupport.park()方法，并检测线程是否被中断过
// 4.若acquireQueued方法获取到资源并返回true，表示被中断过，则执行线程自我中断操作selfInterrupt()，即获取资源后中断，来响应中断请求
```

### 释放资源（独占）

```java
public final boolean release(int arg) {
    if (tryRelease(arg)) {
        // 获取等待队列的头结点h
        Node h = head;
        // 若头结点不为空且其ws值非0，则唤醒h的后继节点
        if (h != null && h.waitStatus != 0)
            unparkSuccessor(h);
        return true;
    }
    return false;
}

// 尝试以独占的模式释放资源，具体实现由扩展了AQS的同步器完成
protected boolean tryRelease(int arg) {
    throw new UnsupportedOperationException();
}

/*
 * 唤醒后继节点
 */
private void unparkSuccessor(Node node) {
    /*
     * 调整当前节点的状态
     */
    int ws = node.waitStatus;
    if (ws < 0)
        compareAndSetWaitStatus(node, ws, 0);

    /*
     * 若后继节点被取消或为null,则从尾部开始寻找真正的未被取消的节点
     */
    Node s = node.next;
    if (s == null || s.waitStatus > 0) {
        s = null;
        for (Node t = tail; t != null && t != node; t = t.prev)
            if (t.waitStatus <= 0)
                s = t;
    }
    if (s != null)
        // 唤醒线程
        LockSupport.unpark(s.thread);
}
```

### 获取资源（共享模式）

```java
public final void acquireShared(int arg) {
    if (tryAcquireShared(arg) < 0)
        doAcquireShared(arg);
}

// 尝试以共享的模式释放资源，具体实现由扩展了AQS的同步器完成
protected int tryAcquireShared(int arg) {
    throw new UnsupportedOperationException();
}

private void doAcquireShared(int arg) {
    // 将线程以共享模式添加到等待队列的尾部
    final Node node = addWaiter(Node.SHARED);
    // 初始化标识是否获取资源失败
    boolean failed = true;
    try {
        // 初始化失败标志
        boolean interrupted = false;
        // 自旋操作
        for (;;) {
            // 获取当前节点的前继节点
            final Node p = node.predecess();
            // 若前继节点为头节点，则执行tryAcquireShared获取资源
            if (p == head) {
                int r = tryAcquireShared(arg);
                // 若获取资源成功，且有剩余资源，将自己设为头结点并唤醒后继的阻塞线程
                if (r >= 0) {
                    setHeadAndPropagate(node, r);
                    p.next = null;
                    if (interrupted)
                        selfInterrupt();
                    failed = false;
                    return;
                }
            }
            // 若获取资源失败 调用shouldParkAfterFailedAcquire方法判断是否阻塞当前节点的线程
            // 调用parkAndCheckInterrupt方法检查线程是否被中断过
            if (shouldParkAfterFailedAcquire(p, node) &&
                    parkAndCheckInterrupt()) 
                interrupted = true;
        }
    } finally {
        if (failed)
            // 放弃获取资源
            cancelAcquire(node);
    }
}

private void setHeadAndPropagate(Node node, int propagate) {
    // 记录原来的头节点
    Node h = head;
    // 设置新的头节点
    setHead(node);
    
    // 如果资源还有剩余，则唤醒后继节点
    if (propagate > 0 || h == null || h.waitStatus < 0 ||
       (h = head) == null || h.waitStatus < 0) {
        Node s = node.next;
        if (s == null || s.inShared())
            doReleaseShared();
    }
}

private void doReleaseShared() {
    // 自旋操作
    for (;;) {
        // 获取等待队列的头节点
        Node h = head;
        // 等待队列至少含两个节点
        if (h != null && h != tail) {
            int ws = h.waitStatus;
            if (ws == Node.SIGNAL) {
                if (!compareAndSetWaitStatus(h, Node.SIGNAL, 0))
                    continue;            // loop to recheck cases
                unparkSuccessor(h);
            }
            else if (ws == 0 &&
                     !compareAndSetWaitStatus(h, 0, Node.PROPAGATE))
                continue;                // loop on failed CAS
        }
        // 若head已被其他线程修改，继续自旋判断下一个新的head
        if (h == head)                   // loop if head changed
            break;
    }
}
```

### 释放资源（共享模式）

```java
public final boolean releaseShared(int arg) {
    if (tryReleaseShared(arg)) {
        doReleaseShared();
        return true;
    }
    return false;
}
```

### 条件队列

在介绍内部类 Node 时有提到一个变量 nextWaiter，这就是AQS中另一个重要组成部分 条件队列。

#### 内部类 ConditionObject

```java
// 条件队列是单链表结构
private transient Node firstWaiter; // 条件队列首节点
private transient Node lastWaiter; // 条件队列尾节点

private static final int REINTERRUPT = 1; // 从等待状态切换为中断状态
private static final int THROW_IE = -1; // 抛出异常标识
```

#### 线程等待 await()

```java
public final void await() throws InterruptedException {
    if (Thread.interrupted())
        throw new InterruptedException();
    Node node = addConditionWaiter(); // 向条件等待队列中添加新节点
    // 将新节点加入到等待队列之后，需要去释放锁，并且唤醒后继节点线程
    int saveState = fullyRelease(node);
    int interruptMode = 0;
    // 当前节点不在同步队列中
    while (!isOnSyncQueue(node)) {
        LockSupport.park(this); //阻塞当前线程，等待被其他线程唤醒 或 中断
        // 线程被唤醒后判断线程若为中断状态，则尝试将node节点状态变更为0
        // 若变更成功，判断中断原因是否是异常，若变更失败，则让出CPU，让其他线程将node放回同步队列
        // 若返回是初始值0，则判断是否进入同步队列，结束循环
        if ((interruptMode = checkInterruptWhileWaiting(node)) != 0)
            break;
    }
    // 让节点线程申请锁，若申请成功调整interruptMode值，未来会让线程中断
    if (acquireQueued(node, savedState) && interruptMode != THROW_IE)
        interruptMode = REINTERRUPT;
    if (node.nextWaiter != null)
        unlinkCancelledWaiters();
    if (interruptMode != 0)
        // 处理interruptMode不为初始值的情况 抛异常或中断
        reportInterruptAfterWait(interruptMode);
}

private Node addConditionWaiter() {
    Node t = lastWaiter;
    if (t != null && t.waitStatus != Node.CONDITION) {
        unlinkCancelledWaiters(); // 遍历等待队列，将非CONDITION状态的节点移除
        t = lastWaiter;
    }
    Node node = new Node(Thread.currentThread(), Node.CONDITION);
    // 条件队列为空
    if (t == null)
        firstWaiter = node;
    else
        // 尾插
        t.nextWaiter = node;
    lastWaiter = node;
    return node;
}
final int fullyRelease(Node node) {
    boolean failed = true;
    try {
        int savedState = getState(); 
        // 调用 release 方法，返回保存的状态，失败则抛出异常且将节点置为取消状态
        if (release(savedState)) {
            failed = false;
            return savedStatus;
        } else {
            throw new IllegalMonitorStateException();
        }
    } finally {
        if (failed)
            node.waitStatus = Node.CANCELLED;
    }
}
private int checkInterruptWhileWaiting(Node node) {
    return Thread.interrupted() ?
        (transferAfterCancelledWait(node) ? THROW_IE : REINTERRUPT) : 0;
}
final boolean transferAfterCancelledWait(Node node) {
    if (compareAndSetWaitStatus(node, Node.CONDITION, 0)) { // 将节点状态由CONDITION调整为0
        enq(node); // 加入同步队列
        return true;
    }
    while (!isOnSyncQueue(node)) // 让线程回到同步队列
        Thread.yield(); // 让出CPU时间
    return false;
}
private void reportInterruptAfterWait(int interruptMode) throws InterruptedException {
    if (interruptMode == THROW_IE)
        throw new InterruptedException();
    else if (interruptMode == REINTERRUPT)
        selfInterrupt();
}
```

#### 线程唤醒 signal()

```java
public final void signal() {
    if (!isHeldExclusively()) //子类实现具体方法，判断当前线程是否持有锁
        throw new IllegalMonitorStateException();
    Node first = firstWaiter;
    if (first != null)
        doSignal(first); // 执行唤醒操作
}

//唤醒条件队列首节点
private void doSignal(Node first) {
    do {
        // 清空等待队列
        if ((firstWaiter = first.nextWaiter) == null)
            lastWaiter = null;
        first.nextWaiter = null;
    } while (!transferForSignal(first) && // 不断尝试唤醒等待队列的头节点，直到找到一个没有被cancel的节点
            (first = firstWaiter) != null);
}
final boolean transferForSignal(Node node) {
    // CAS 将当前条件队列节点状态设置为0
    if (!compareAndSetWaitStatus(node, Node.CONDITION,0))
        return false;
    // transfer至同步队列 返回node的前一节点
    Node p = enq(node);
    int ws = p.waitStatus;
    if (ws > 0 || !compareAndSetWaitStatus(p, ws, Node.SIGNAL))
        //唤醒刚加入到同步队列的线程，被唤醒之后，该线程才能从await()方法的park()中返回
        LockSupport.unpark(node.thread); 
    return true;
}
```

#### 唤醒所有线程signalAll()

```java
public final void signalAll() {
    if (!isHeldExclusively())
        throw new IllegalMonitorStateException();
    Node first = firstWaiter;
    if (first != null)
        doSignalAll(first);
}

private void doSignalAll(Node first) {
    lastWaiter = firstWaiter = null;
    do {
        Node next = first.nextWaiter;
        first.nextWaiter = null;
        transferForSignal(first);
        first = next;
    } while (first != null);
}
```

