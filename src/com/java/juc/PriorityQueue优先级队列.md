## PriorityQueue优先级队列

### 成员变量

```java
private static final int DEFAULT_INITIAL_CAPACITY = 11; // 默认初始容量
// 优先级队列底层是二叉平衡堆，queue[n]的左孩子是queue[2*n+1]，右孩子是queue[2*(n+1)]
transient Object[] queue;
private int size = 0; // 优先级队列当前容量
private final Comparator<? super E> comparator; // 比较器
transient int modCount = 0;// 优先级队列修改次数
```

### 构造函数

```java
public PriorityQueue() { this(DEFAULT_INITIAL_CAPACITY, null); }
public PriorityQueue(int initialCapacity) { this(initialCapacity, null); }
public PriorityQueue(Comparator<? super E> comparator) { this(DEFAULT_INITIAL_CAPACITY, comparator);}
public PriorityQueue(int initialCapacity, Comparator<? super E> comparator) {
    if (initialCapacity < 1)
        throw new IllegalArgumentException();
    this.queue = new Object[initialCapacity];
    this.comparator = comparator;
}
// 根据传入的Collection来构造
public PriorityQueue(Collection<? extends E> c) {
    // 若传入SortedSet实例
    if (c instanceof SortedSet<?>) {
        SortedSet<? extends E> ss = (SortedSet<? extends E>) c;
        this.comparator = (Comparator<? super E>) ss.comparator(); // 获取比较器
        initElementsFromCollection(ss); // 赋值到Object[] queue
    }
    // 若传入PriorityQueue实例
    else if (c instanceof PriorityQueue<?>) {
        PriorityQueue<? extends E> pq = (PriorityQueue<? extends E>) c;
        this.comparator = (Comparator<? super E>) pq.comparator(); // 获取比较器
        initFromPriorityQueue(pq); // 赋值到Object[] queue
    }
    else {
        this.comparator = null;
        initFromCollection(c);
    }
}
// 私有函数
private void initFromCollection(Collection<? extends E> c) {
    // 赋值到Object[] queue
    initElementsFromCollection(c);
    // 堆化
    heapify();
}
```

### 扩容

```java
private grow(int minCapacity) {
    int oldCapacity = queue.length;
    // Double size if small, else grow by 50%
    int newCapacity = oldCapacity + ((oldCapacity < 64) ?
                                    (oldCapacity + 2) :
                                    (oldCapacity >> 1));
}
```

### 堆相关操作

```java
// 插入元素x于位置k，上移k使得其值大于等于父节点，以维持堆结构
private void siftUp(int k, E x) {
    if (comparator != null)
        siftUpUsingComparator(k, x);
    else
        siftUsingComparable(k, x);
}
private void siftUpComparable(int k, E x) {
    Comparable<? super E> key = (Comparable<? super E>) x;
    // 一直向上查找直至根节点
    while(k > 0) {
        // 查找父节点坐标
        int parent = (k - 1) >>> 1;
        Object e = queue[parent];
        if (key.compareTo((E) e) >= 0)
            break;
        // k位置放入父节点值，父节点位置放入key
        queue[k] = e;
        k = parent;
    }
    queue[k] = key;
}
// 插入元素x于位置k，下移k使得其值小于等于孩子节点，以维持堆结构
private void siftDown(int k, E x) {
    if (comparator != null)
        siftDownUsingComparator(k, x);
    else
        siftDownComparable(k, x);
}
private void siftDownComparable(int k, E x) {
    Comparable<? super E> key = (Comparable<? super E>) x;
    // 最后一个非叶子节点位置
    int half = size >>> 1;
    while(k < half) {
        int child = (k << 1) + 1; // 左孩子节点位置
        Object c = queue[child]; // 左孩子
        int right = child + 1; // 右孩子节点位置
        if (right < size &&
           ((Comparable<? super E>) c).compareTo((E) queue[right]) > 0)
            // 取左右孩子中较小的值
            c = queue[child = right];
        // 比较父亲节点与孩子节点
        if (key.compareTo((E) c) <= 0)
            break;
        // 若父亲节点较大，将位置k赋值为孩子节点较小的那个
        queue[k] = c;
        k = child;
    }
    // 将目标插入值插入到对应的位置
    queue[k] = key;
}
// 堆化
private void heapify() {
    // 从最后一个非叶子节点开始遍历，向下构建堆
    for (int i = (size >>> 1) - 1; i >= 0; i--)
        siftDown(i, (E) queue[i]);
}
```

### 队列相关操作

```java
public boolean add(E e) {return offer(e);}
public boolean offer(E e) {
    if (e == null)
        throw new NullPointerException();
    modCount++;
    int i = size;
    // 队列容量已满则扩容
    if (i >= queue.length)
        grow(i + 1);
    size = i + 1;
    if (i == 0)
        queue[0] = e;
    else
        // 从数组最后一个位置开始向上调整堆
        siftUp(i, e);
    return true;
}
public E peek() {return (size == 0) ? null : (E) queue[0]; }
public boolean remove(Object o) {
    int i = indexOf(o);
    if (i == -1)
        return false;
    else {
        removeAt(i);
        return true;
    }
}
private int indexOf(Object o) {
    if (o != null) {
        // 遍历数组
        for (int i = 0; i < size; i++)
            if (o.equals(queue[i]))
                return i;
    }
    return -1;
}
private E removeAt(int i) {
    modCount++;
    int s = --size;
    if (s == i) // removed last element
        queue[i] = null;
    else {
        E moved = (E) queue[s];
        queue[s] = null;
        // 将最后一个元素放到i位置并向下调整
        siftDown(i, moved);
        if (queue[i] == moved) { // 有时调整完毕后i位置元素与最后一个元素相同
            siftUp(i, moved); // 向上调整
            if (queue[i] != moved) // 若向上调整后有大于当前位置元素的，返回
                return moved;
        }
    }
    return null;
}
// 返回队首元素
public E poll() {
    if (size == 0)
        return null;
    int s = --size;
    modCount++;
    E result = (E) queue[0];
    E x = (E) queue[s];
    queue[s] = null;
    if (s != 0)
        // 将最后一个元素放入队首并开始向下调整
        siftDown(0, x);
    return result;
}
```

priorityQueue 底层实现是数组，其使用堆的思想，在构造的时候，数值赋值到数组后，进行heapify，即从最后一个非叶子节点开始向下调整

在删除元素的时候，会把最后一个元素放入删除位置，然后向下开始调整

在添加元素的时候，会在最后一个位置放入元素，并开始向上调整