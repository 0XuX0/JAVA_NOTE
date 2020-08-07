阻塞队列是典型的生产者-消费者模式的应用，阻塞队列解决了多个线程操作共享变量容易引发的重复消费和死锁等线程安全问题，以及生产者-消费者之间等待-唤醒的过程。生产者是往队列添加元素的线程，消费者是从队列里拿元素的线程。

> BlockingQueue 是 Java util.concurrent 包下重要的数据结构，区别于普通的队列，BlockingQueue提供了线程安全的队列访问方式，并发包下很多高级同步类的实现基于BlockingQueue

## BlockingQueue 的操作方法

BlockingQueue是一个接口，继承自Queue接口，提供了以下四组不同的方法用于插入、移除、检查元素：

| 方法\处理方式 | 抛出异常  | 返回特殊值 | 一直阻塞 | 超时退出           |
| ------------- | --------- | ---------- | -------- | ------------------ |
| 插入方法      | add(e)    | offer(e)   | put(e)   | offer(e,time,unit) |
| 移除方法      | remove()  | poll()     | take()   | poll(time,unit)    |
| 检查方法      | element() | peek()     |          |                    |

+ 抛出异常：如果试图的操作无法立即执行，抛异常。

  当阻塞队列满时，再往队列里插入元素会抛出 IllegalStateException 异常

  当阻塞队列为空时，使用移除方法以及检查队首方法会抛出 NoSuchElementException 异常

+ 返回特殊值：如果试图的操作无法立即执行，返回特殊值，插入是false，移除和检查方法是null

+ 一直阻塞：如果试图的操作无法立即执行，一直阻塞或响应中断

+ 超时推出：如果试图的操作无法立即执行，该方法调用将会发生阻塞，直到能够执行，但等待时间不会超过给定值

## BlockingQueue 的实现类

### ArrayBlockingQueue

##### 成员变量

```java
public class ArrayBlockingQueue<E> extends AbstractQueue<E>
    implements BlockingQueue<E>, java.io.Serializable {
    // 存放数据的数组
    final Object[] items;
    // 下一次 take,poll,peek or remove 的索引
    int takeIndex;
    // 下一次 put,offer,add 的索引
    int putIndex;
    // 队列中元素割术
    int count;
    // 可重入锁
    final ReentrantLock lock;
    // 队列不为空的条件
    private final Condition notEmpty;
    // 队列不满的条件
    private final Condition notFull;
    // 标识当前迭代器的共享状态
    transient Itrs itrs = null;
}
```

##### 构造函数

```java
public ArrayBlockingQueue(int capacity) { this(capacity, false); } // 设置初始容量，默认非公平锁
// 设置初始容量和是否为公平锁
public ArrayBlockingQueue(int capacity, boolean fair) {
    if (capacity <= 0)
        throw new IllegalArgumentException();
    this.items = new Object[capacity];
    lock = new ReentrantLock(fair);
    notEmpty = lock.newCondition();
    notFull = lock.newCondition();
}
// 带有初始集合
public ArrayBlockingQueue(int capacity, boolean fair, Collection<? extends E> c) {
    this(capacity, fair);
    final ReentrantLock lock = this.lock;
    lock.lock();
    try {
        int i = 0;
        try {
            for (E e : c) {
                checkNotNull(e);
                items[i++] = e; // 依次拷贝内容
            }
        } catch (ArrayIndexOutOfBoundsException ex) {
            throw new IllegalArgumentException();
        }
        count = i;
        putIndex = (i == capacity) ? 0 : i; // 设置putIndex的值，若c长度刚好为capacity，putIndex应为0
    } finally {
        lock.unlock();
    }
}
```

##### 插入方法 

```java
public boolean add(E e) {
    // 父类abstractQueue是调用arrayBlockingQueue的offer方法，若为false则抛出异常
    return supter.add(e);
}
public boolean offer(E e) {
    checkNotNull(e);
    final ReentrantLock lock = this.lock;
    lock.lock();
    try {
        // queue已经满了
        if (count == items.length)
            return false;
        else {
            // 入队操作
            enqueue(e);
            return true;
        }
    } finally {
        lock.unlock();
    }
}
public void put(E e) throws InterruptedException {
    checkNotNull(e);
    final ReentrantLock lock = this.lock;
    lock.lockInterruptibly();
    try {
        // queue已经满了，放入notFull等待队列
        while (count == items.length)
            notFull.await();
        // 入队操作
        enqueue(e);
    } finally {
        lock.unlock();
    }
}
public boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException {
    checkNotNull(e);
    long nanos = unit.toNanos(timeout);
    final ReentrantLock lock = this.lock;
    lock.lockInterruptibly();
    try {
        // 数组有空余位置，直接enqueue，若已满则放入notFull等待队列中等待一段时间，超时返回失败
        while (count == items.length) {
            if (nanos <= 0)
                return false;
            nanos = notFull.awaitNanos(nanos);
        }
        enqueue(e);
        return true;
    } finally {
        lock.unlock();
    }
}

// 私有方法 enqueue
private void enqueue(E x) {
    final Object[] items = this.items;
    items[putIndex] = x;
    // 若队列已满 则更新 putIndex 为0
    if (++putIndex == items.length)
        putIndex = 0;
    count++;
    // 将notEmpty等待队列中的线程唤醒
    notEmpty.signal();
} 
```

##### 移除方法

```java
public boolean remove(Object o) {
    if (o == null) return false;
    final Object[] items = this.items;
    final ReentrantLock lock = this.lock;
    lock.lock();
    try {
        if (count > 0) {
            final int putIndex = this.putIndex;
            int i = takeIndex;
            // 正常情况下 putIndex 是要大于 takeIndex，或者两者都为0的情况
            // 从 takeIndex 出发，迭代寻找需要移除的目标对象，直至两个index值相同
            do {
                if (o.equals(items[i])) {
                    removeAt(i);
                    return true;
                }
                if (++i == items.length)
                    i = 0;
            } while (i != putIndex);
        }
        return false;
    } finally {
        lock.unlock();
    }
}

public E poll() {
    final ReentrantLock lock = this.lock;
    lock.lock();
    try {
        return (count == 0) ? null : dequeue();
    } finally {
        lock.unlock();
    }
}

public E take() throws InterruptedException {
    final ReentrantLock lock = this.lock;
    lock.lockInterruptibly();
    try {
        // 若数组中没有元素，将当前线程加入notEmpty条件队列中等待被唤醒
        while (count == 0)
            notEmpty.await();
        return dequeue();
    } finally {
        lock.unlock();
    }
}

public E poll(long timeout, TimeUnit unit) throws InterruptedException {
    long nanos = unit.toNanos(timeout);
    final ReentrantLock lock = this.lock;
    lock.lockInterruptibly();
    try {
        // 若数组中没有元素，将当前线程加入notEmpty条件队列中等待，设置超时时间
        while (count == 0) {
            // 超时时间已到 返回null
            if (nanos <= 0)
                return null;
            nanos = notEmpty.awaitNanos(nanos);
        }
        return dequeue();
    } finally {
        lock.unlock();
    }
}

// 私有方法 dequeue
private E dequeue() {
    final Object[] items = this.items;
    E x = (E) items[takeIndex];
    items[takeIndex] = null;
    // 若队列已满 则更新 takeIndex 为0
    if (++takeIndex == items.length)
        takeIndex = 0;
    count--;
    if (itrs != null)
        itrs.elementDequeued();
    // 将notFull等待队列中的线程唤醒
    notFull.signal();
    return x;
}
```

##### 检查方法

```java
public E peek() {
    final ReentrantLock lock = this.lock;
    lock.lock();
    try {
        return itemAt(takeIndex);
    } finally {
        lock.unlock();
    }
}
```

### linkedBlockingQueue

##### 成员变量

```java
static class Node<E> {
    E item;
    Node<E> next;
    Node(E x) { item = x; }
}
private final int capacity;// 默认为 Integer.MAX_VALUE
private final AtomicInteger count = new AtomicInteger();
transient Node<E> head;// 链表头部节点
private transient Node<E> last;// 链表尾部节点
private final ReentrantLock takeLock = new ReentrantLock();//出队锁
private final Condition notEmpty = takeLock.newCondition();//队列非空条件
private final ReentrantLock putLock = new ReentrantLock();//入队锁
private final Condition notFull = putLock.newCondition();//队列不满条件
```

##### 构造函数

```java
public LinkedBlockingQueue() { this(Integer.MAX_VALUE); }
public LinkedBlockingQueue(int capacity) {
    if (capacity <= 0) throw new IllegalArgumentException();
    this.capacity = capacity;
    last = head = new Node<E>(null);
}
public LinkedBlockingQueue(Collection<? extends E> c) {
    this(Integer.MAX_VALUE);
    final ReentrantLock putLock = this.putLock;
    // 使用put锁
    putLock.lock();
    try {
        int n = 0;
        for (E e : c) {
            if (e == null)
                throw new NullPointerException();
            if (n == capacity)
                throw new IllegalStateException("Queue full");
            enqueue(new Node<E>(e));
            ++n;
        }
        count.set(n);
    } finally {
        putLock.unlock();
    }
}
```

##### 插入方法

```java
public void put(E e) throws InterruptedException {
    if (e == null) throw new NullPointerException();
    int c = -1;
    Node<E> node = new Node<E>(e);
    final ReentrantLock putLock = this.putLock;
    final AtomicInteger count = this.count;
    putLock.lockInterruptibly();
    try {
        // 若队列已满，则进入notFull条件队列，一直阻塞等待被唤醒
        while (count.get() == capacity) {
            notFull.await();
        }
        // 入队操作
        enqueue(node);
        c = count.getAndIncrement();
        // 若队列还有空余位置，唤醒notFull条件队列中等待的线程
        if (c + 1 < capacity)
            notFull.signal();
    } finally {
        putLock.unlock();
    }
    // 说明在释放锁的时候，队列里至少有一个元素，唤醒notEmpty条件队列中等待的线程，继续消费
    if (c == 0)
        signalNotEmpty();
}
public boolean offer(E e) {
    if (e == null) throw new NullPointerException();
    final AtomicInteger count = this.count;
    if (count.get() == capacity)
        return false;
    int c = -1;
    Node<E> node = new Node<E>(e);
    final ReentrantLock putLock = this.putLock;
    putLock.lock();
    try {
        if (count.get() < capacity) {
            enqueue(node);
            c = count.getAndIncrement();
            if (c + 1 < capacity)
                notFull.signal();
        }
    } finally {
        putLock.unlock();
    }
    if (c == 0)
        signalNotEmpty();
    return c >= 0;
}
private void enqueue(Node<E> node) {
    last = last.next = node;
}
```

##### 移除方法

```java
public E take() throws InterruptedException {
    E x;
    int c = -1;
    final AtomicInteger count = this.count;
    final ReentrantLock takeLock = this.takeLock;
    takeLock.lockInterruptibly();
    try {
        // 若队列为空，则进入notEmpty条件队列，一直阻塞等待被唤醒
        while (count.get() == 0) {
            notEmpty.await();
        }
        // 出队操作
        x = dequeue();
        c = count.getAndDecrement();
        // 若队列有元素，唤醒notEmpty条件队列中等待的线程
        if (c > 1)
            notEmpty.signal();
    } finally {
        takeLock.unlock();
    }
    // 说明在释放锁的时候，队列里是满的，现减少一个元素后，唤醒notFull条件队列中等待的线程，继续生产
    if (c == capacity)
        signalNotFull();
    return x;
}
public E poll() {
    final AtomicInteger count = this.count;
    if (count.get() == 0)
        return null;
    E x = null;
    int c = -1;
    final ReentrantLock takeLock = this.takeLock;
    takeLock.lock();
    try {
        if (count.get() > 0) {
            x = dequeue();
            c = count.getAndDecrement();
            if (c > 1)
                notEmpty.signal();
        }
    } finally {
        takeLock.unlock();
    }
    if (c == capacity)
        signalNotFull();
    return x;
}

private E dequeue() {
    Node<E> h = head;
    Node<E> first = h.next;
    h.next = h; // help GC 
    head = first;
    E x = first.item;
    first.item = null;
    return x;
}

public boolean remove(Object o) {
    if (o == null) return false;
    fullyLock();
    try {
        // 遍历链表找到目标节点
        for (Node<E> trail = head, p = trail.next;
            p != null;
            trail = p, p = p.next) {
            if (o.equals(p.item)) {
                // 移除操作
                unlink(p, trail);
                return true;
            }
        }
        return false;
    } finally {
        fullyUnlock();
    }
}
void fullyLock() {
    putLock.lock();
    takeLock.lock();
}
void fullyUnlock() {
    putLock.unlock();
    takeLock.unlock();
}
void unlink(Node<E> p, Node<E> trail) {
    p.item = null;
    // 移除
    trail.next = p.next;
    // 尾指针处理
    if (last == p)
        last = trail;
    // 通知notFull等待队列唤醒线程，继续生产
    if (count.getAndDecrement() == capacity)
        notFull.signal();
}
```

##### 检查方法

```java
public E peek() {
    if (count.get() == 0)
        return null;
    final ReentrantLock takeLock = this.takeLock;
    takeLock.lock();
    try {
        Node<E> first = head.next;
        if (first == null)
            return null;
        else
            return first.item;
    } finally {
        takeLock.unlock();
    }
}
```

