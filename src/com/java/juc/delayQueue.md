## delayQueue

### 成员变量

```java
public class DelayQueue<E extends Delayed> extends AbstractQueue<E> implements BlockingQueue<E> {
    // 插入DelayQueue的元素需实现Delayed接口
    private final transient ReentrantLock lock = new ReentrantLock(); // 同步锁
    private final PriorityQueue<E> q = new PriorityQueue<E>(); // 优先队列
    private Thread leader = null;
    private final Condition available = lock.newCondition();
}
```

### 构造函数

```java
public DelayQueue() {}
public DelayQueue(Collection<? extends E> c) { this.addAll(c); }
```

### 添加方法

```java
// priorityQueue 会扩容，不会阻塞
public boolean add(E e) { return offer(e); }
public void put(E e) { offer(e); }
public boolean offer(E e) {
    final ReentrantLock lock = this.lock;
    lock.lock();
    try {
        q.offer(e);
        if (q.peek() == e) {
            leader = null;
            available.signal();
        }
    } finally {
        lock.unlock();
    }
}
public boolean offer(E e, long timeout, TimeUnit unit) {
    return offer(e);
}
```

### 移除方法

```java
public E poll() {
    final ReentrantLock lock = this.lock;
    lock.lock();
    try {
        E first = q.peek();
        if (first == null || first.getDelay(NANOSECONDS) > 0) // 取出优先队列的首元素，看其是否已到期
            return null;
        else
            return q.poll();
    } finally {
        lock.unlock();
    }
}
public E take() throws InterruptedException {
    final ReentrantLock lock = this.lock;
    lock.lockInterruptibly();
    try {
        for (;;) {
            E first = q.peek();
            if (first == null) // 说明队列为空，调用condition.await()方法，使当前线程释放lock进入等待队列
                available.await();
            else {
                long delay = first.getDelay(NANOSECONDS);
                if (delay <= 0)
                    return q.poll();
                // 如果延迟时间不为空，说明还需要等待一段时间，此时重新循环，需要将first置为空
                first = null;
                if (leader != null)
                    // Leader/Followers模式
                    // 若leader不为空，说明已有线程在监听，即有线程在优先获取队列的首元素
                    // 释放当前线程的锁，加入到等待队列中，即当前线程变成了Followers
                    available.await();
                else {
                    // 没有leader，将自身设置为leader
                    Thread thisThread = Thread.currentThread();
                    leader = thisThread;
                    try {
                        // 让当前线程最长等待delay时间
                        available.awaitNanos(delay);
                    } finally {
                        // 释放leader权限
                        if (leader == thisThread)
                            leader = null;
                    }
                }
            }
        }
    } finally {
        // 如果leader为空且队列中有数据，说明没有其他线程在等待
        if (leader == null && q.peek() != null)
            // 唤醒睡眠的线程
            available.signal();
        lock.unlock();
    }
}
```

