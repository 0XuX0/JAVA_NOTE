## PriorityBlockingQueue

### 成员变量

```java
private static final int DEFAULT_INITIAL_CAPACITY = 11; // 默认初始容量
private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8; // 数组最大容量
// 优先级阻塞队列底层是二叉平衡堆，queue[n]的左孩子是queue[2*n+1]，右孩子是queue[2*(n+1)]
private transient Obejct[] queue; 
private transient int size; // 优先级阻塞队列当前容量
private transient Comparator<? super E> comparator; // 比较器
private final ReentrantLock lock; // 同步锁
private final Condition notEmpty; // 队列为空时等待
private transient volatile int allocationSpinLock; // CAS自旋锁
private PriorityQueue<E> q; // 用作序列化
```

### 构造函数

```java
public PriorityBlockingQueue() { this(DEFAULT_INITAL_CAPACITY, null); }
public PriorityBlockingQueue(int initialCapacity) { this(initialCapacity, null); }
public PriorityBlockingQueue(int initialCapacity, Comparator<? super E> comparator) {
    if (initialCapacity < 1)
        throw new IllegalArgumentException();
    this.lock = new ReentrantLock();
    this.notEmpty = lock.newCondition();
    this.comparator = comparator;
    this.queue = new Object[initialCapacity];
}
public PriorityBlockingQueue(Collection<? extends E> c) {
    this.lock = new ReentrantLock();
    this.notEmpty = lock.newCondition();
    boolean heapify = true; // true if not known to be in heap order
    boolean screen = true;  // true if must screen for nulls
    if (c instanceof SortedSet<?>) {
        // 若为有序集合，则不需要进行堆有序化
        SortedSet<? extends E> ss = (SortedSet<? extends E>) c;
        this.comparator = (Comparator<? super E>) ss.comparator();
        heapify = false;
    }
    else if (c instanceof PriorityBlockingQueue<?>) {
        PriorityBlockingQueue<? extends E> pq =
            (PriorityBlockingQueue<? extends E>) c;
        this.comparator = (Comparator<? super E>) pq.comparator();
        screen = false;
        if (pq.getClass() == PriorityBlockingQueue.class) // exact match
            heapify = false;
    }
    Object[] a = c.toArray();
    int n = a.length;
    // If c.toArray incorrectly doesn't return Object[], copy it.
    if (a.getClass() != Object[].class)
        a = Arrays.copyOf(a, n, Object[].class);
    // 检查元素是否有空元素
    if (screen && (n == 1 || this.comparator != null)) {
        for (int i = 0; i < n; ++i)
            if (a[i] == null)
                throw new NullPointerException();
    }
    this.queue = a;
    this.size = n;
    if (heapify)
        // 执行堆有序化
        heapify();
}
```

### 扩容

```java
private void tryGrow(Object[] array, int oldCap) {
    // 扩容时，释放锁，防止阻塞出队操作
    lock.unlock(); // must release and then re-acquire main lock
    Object[] newArray = null;
    // 自旋锁
    if (allocationSpinLock == 0 && UNSAFE.compareAndSwapInt(this, allocationSpinLockOffset, 0, 1)) {
        try {
            int newCap = oldCap + ((oldCap < 64) ?
                                   (oldCap + 2) : // grow faster if small
                                   (oldCap >> 1));
            if (newCap - MAX_ARRAY_SIZE > 0) {    // possible overflow
                int minCap = oldCap + 1;
                if (minCap < 0 || minCap > MAX_ARRAY_SIZE)
                    throw new OutOfMemoryError();
                newCap = MAX_ARRAY_SIZE;
            }
            if (newCap > oldCap && queue == array)
                newArray = new Object[newCap];
        } finally {
            allocationSpinLock = 0;
        }
    }
    // 如果此时有其他线程已加锁，则让出执行权，等待其他线程完成
    if (newArray == null) // back off if another thread is allocating
        Thread.yield();
    // 加锁，替换现有数组的引用，拷贝数组元素
    lock.lock();
    if (newArray != null && queue == array) {
        queue = newArray;
        System.arraycopy(array, 0, newArray, 0, oldCap);
    }
}
```

### 堆上浮下沉操作

```java
// 参考 priorityQueue
```

### 队列相关操作

```java
// 入队不会阻塞
public boolean add(E e) { return offer(e); }
public put(E e) { offer(e); }
public boolean offer(E e, long timeout, TimeUnit unit) { return offer(e); }
public boolean offer(E e) {
    if (e == null)
        throw new NullPointerException();
    final ReentrantLock lock = this.lock;
    lock.lock();
    int n, cap;
    Object[] array;
    // 动态扩容
    while((n = size) >= (cap = (array = queue).length))
        tryGrow(array, cap);
    try {
        Comparator<? super E> cmp = comparator;
        // 堆最后一个部位添加元素，向上调整
        if (cmp == null)
            siftUpComparable(n, e, array);
        else
            siftUpUsingComparator(n, e, array, cmp);
        size = n + 1;
        // 唤醒 notEmpty 等待队列等待的线程
        notEmpty.signal();
    } finally {
        lock.unlock();
    }
    return true;
}

// 出队
public E poll() {
    final ReentrantLock lock = this.lock;
    lock.lock();
    try {
        return dequeue();
    } finally {
        lock.unlock();
    }
}
public E take() throws InterruptedException {
    final ReentrantLock lock = this.lock;
    lock.lockInterruptibly(); // 可中断锁
    E result;
    try {
        // 队列为空 阻塞等待
        while((result = dequeue()) == null)
            notEmpty.await();
    } finally {
        lock.unlock();
    }
    return result;
}
public E poll(long timeout, TimeUnit unit) throws InterruptedException {
    long nanos = unit.toNanos(timeout);
    final ReentrantLock lock = this.lock;
    lock.lockInterruptibly();
    E result;
    try {
        // 队列为空则等待nanos时间
        while ( (result = dequeue()) == null && nanos > 0)
            nanos = notEmpty.awaitNanos(nanos);
    } finally {
        lock.unlock();
    }
    return result;
}
public boolean remove(Object o) {
    final ReentrantLock lock = this.lock;
    lock.lock();
    try {
        int i = indexOf(o);
        if (i == -1)
            return false;
        removeAt(i);
        return true;
    } finally {
        lock.unlock();
    }
}

// 私有方法
private E dequeue() {
    int n = size - 1;
    if (n < 0) 
        return null;
    else {
        Object[] array = queue;
        E result = (E) array[0];
        E x = (E) array[n];
        // 将队尾元素置为空
        array[n] = null;
        Comparator<? super E> cmp = comparator;
        // 队尾元素放到队首，向下调整堆
        if (cmp == null)
            siftDownComparable(0, x, array, n);
        else
            siftDownUsingComparator(0, x, array, n, cmp);
        size = n;
        return result;
    }
}
private void removeAt(int i) {
    Object[] array = queue;
    int n = size - 1;
    if (n == i) // removed last element
        array[i] = null;
    else {
        E moved = (E) array[n];
        array[n] = null;
        Comparator<? super E> cmp = comparator;
        if (cmp == null)
            siftDownComparable(i, moved, array, n);
        else
            siftDownUsingComparator(i, moved, array, n, cmp);
        if (array[i] == moved) {
            if (cmp == null)
                siftUpComparable(i, moved, array);
            else 
                siftUpUsingComparator(i, moved, array, cmp);
        }
    }
    size = n;
}
```

