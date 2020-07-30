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
// 3.若acquireQueued方法获取到资源并返回true，表示被中断过，则执行线程自我中断操作selfInterrupt()，即获取资源后中断，来响应中断请求
```



```java
public abstract class AbstractQueuedSynchronizer extends AbstractOwnableSynchronizer {
    
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
    
    private void doReleaseShared() {
        for (;;) {
            Node h = head;
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
            if (h == head)                   // loop if head changed
                break;
        }
    }
    
    private void setHeadAndPropagate(Node node, int propagate) {
        Node h = head;
        setHead(node);
        
        if(propagate > 0 || h == null || h.waitStatus < 0 ||
          (h = head) == null || h.waitStatus < 0) {
            Node s = node.next;
            if(s == null || s.isShared())
                doReleaseShared();
        }
    }
    
}
```

