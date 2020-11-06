ConcurrentHashMap

成员变量

```java
private static final int MAXIMUM_CAPACITY = 1 << 30; // 最大容量
private static final int DEFAULT_CAPACITY = 16; // 初始默认容量
static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8; // 数组最大长度
private static final int DEFAULT_CONCURRENCY_LEVEL = 16; // 初始默认并发度
private static final float LOAD_FACTOR = 0.75f; // 负载因子
static final int TREEIFY_THRESHOLD = 8; // 树化阈值
static final int UNTREEIFY_THRESHOLD = 6; // 树退化阈值
static final int MIN_TREEIFY_CAPACITY = 64; // 最小树化容量
private static final int MIN_TRANSFER_STRIDE = 16; 
private static int RESIZE_STAMP_BITS = 16;
private static final int MAX_RESIZERS = (1 << (32 - RESIZE_STAMP_BITS)) - 1;
private static final int RESIZE_STAMP_SHIFT = 32 - RESIZE_STAMP_BITS;

static final int MOVED = -1;
static final int TREEBIN = -2;
static final int RESERVED = -3;
static final int HASH_BITS = 0x7fffffff;
```

构造函数

```java
public ConcurrentHashMap() {}
public ConcurrentHashMap(int initialCapacity) {
    if (initialCapacity < 0)
        throw new IllegalArgumentException();
    int cap = ((initialCapacity >= (MAXIMUM_CAPACITY >>> 1)) ?
              MAXIMUM_CAPACITY :
              tableSizeFor(initialCapacity + (initialCapacity >>> 1) + 1));
    this.sizeCtl = cap;
}
public ConcurrentHashMap(Map<? extends K, ? extends V> m) {
    this.sizeCtl = DEFAULT_CAPACITY;
    putAll(m);
}
public ConcurrentHashMap(int initialCapacity, float loadFactor) {
    this(initialCapacity, loadFactor, 1);
}
public ConcurrentHashMap(int initialCapacity, float loadFactor, int concurrencyLevel) {
    if(!(loadFactor > 0.0f) || initialCapacity < 0 || concurrencyLevel <= 0)
        throw new IllegalArgumentException();
    if(initialCapacity < concurrencyLevel)
        initialCapacity = concurrencyLevel;
    long size = (long)(1.0 + (long)initialCapacity / loadFactor);
    int cap = (size >= (long)MAXIMUM_CAPACITY) ?
        MAXIMUM_CAPACITY : tableSizeFor((int)size);
    this.sizeCtl = cap;
}
```

put操作

```java
public V put(K key, V value) { return putVal(key, value, false); }
final V putVal(K key, V value, boolean onlyIfAbsent) {
    if (key == null || value == null) throw new NullPointerException(); // K,V都不能为空，否则抛出异常
    int hash = spread(key.hashCode()); // 获取key的hash值
    int binCount = 0; // 计算该节点后共有多少元素
    for (Node<K,V>[] tab = table;;) {
        Node<K,V> f; int n, i, fh;
        if (tab == null || (n = tab.length) == 0) // 第一次put时初始化table
            tab = initTable();
        else if ((f = tabAt(tab, i = (n - 1) & hash)) == null) {
            // 通过hash计算数组中的位置，若为空，通过CAS添加，没有加锁
            if (casTabAt(tab, i, null,
                        new Node<K,V>(hash, key, value, null)))
                break;
        }
        else if ((fh = f.hash) == MOVED)
            // 若数组对应位置不为空，但是处于MOVED状态，表示正在进行数组扩容
            // 当前线程也会参与复制转移
            tab = helpTransfer(tab, f);
        else {
            V oldVal = null;
            // 同步块加锁
            synchronized(f) {
                // 再次取出数组对应位置元素二次比较
                if (tabAt(tab, i) == f) {
                    // hash值大于0，当转为树以后，hash值是-2
                    if (fh >= 0) {
                        binCount = 1;
                        for (Node<K,V> e = f;; ++binCount) { // 遍历链表
                            K ek;
                            if (e.hash == hash &&
                               ((ek = e.key) == key ||
                               (ek != null && key.equals(ek)))) {
                                oldVal = e.val;
                                if (!onlyIfAbsent)
                                    e.val = value;
                                break;
                            }
                            Node<K,V> pred = e;
                            if ((e = e.next) == null) { // 尾插法
                                pred.next = new Node<K,V>(hash, key,value,null);
                                break;
                            }
                        }
                    }
                    else if (f instanceof TreeBin) { // 表示数组当前节点已经是红黑树结构
                        Node<K,V> p;
                        binCount = 2;
                        if ((p = ((TreeBin<K,V>)f).putTreeVal(hash,key,value)!=null) {
                            oldVal = p.val;
                            if (!onlyIfAbsent)
                                p.val = value;
                        }
                    }
                }
            }
            if (binCount != 0) {
                if (binCount >= TREEIFY_THRESHOLD) // 当在同一个节点后的节点数目超过8个，调treeifyBin
                    treeifyBin(tab, i);
                if (oldVal != null)
                    return oldVal;
                break;
            }
        }
    }
    addCount(1L, binCount);
    return null;
}
```

扩容操作

```java
private final void treeifyBin(Node<K,V>[] tab, int index) {
    Node<K,V> b; int n, sc;
    if (tab != null) {
        if ((n = tab.length) < MIN_TREEIFY_CAPACITY)
            tryPresize(n << 1); // 数组扩容
        else if ((b = tabAt(tab, index)) != null && b.hash >= 0) {
            // 同步块加锁
            synchronized(b) {
                if (tabAt(tab, index) == b) { // 二次检查
                    TreeNode<K,V> hd = null, tl = null;
                    for (Node<K,V> e = b; e != null; e = e.next) {
                        TreeNode<K,V> p = new TreeNode<K,V>(e.hash, e.key, e.val, null, null);
                        // 把Node组成的链表，转化为TreeNode的链表
                        if ((p.prev = tl) == null)
                            hd = p;
                        else
                            tl.next = p;
                        tl = p;
                    }
                    setTabAt(tab, index, new TreeBin<K,V>(hd)); // 把TreeNode的链表放入容器TreeBin中
                }
            }
        }
    }
}
```

```java
private final void tryPresize(int size) {
    // 如果给定的大小大于等于数组容量的一半，直接使用最大容量，否则使用tableSizeFor计算
    int c = (size >= (MAXIMUM_CAPACITY >>> 1)) ? MAXIMUM_CAPACITY : 
    tableSizeFor(size + (size >>> 1) + 1);
    int sc;
    while((sc = sizeCtl) >= 0) {
        Node<K,V>[] tab = table; int n;
        // 初始化数组，设置sizeCtl为数组长度的 3/4
        if (tab == null || (n = tab.length) == 0) {
            n = (sc > c) ? sc : c;
            if (U.compareAndSwapInt(this, SIZECTL, sc, -1)) {
                try {
                    if (table == tab) {
                        Node<K,V>[] nt = (Node<K,V>[])new Node<?,?>[n];
                        table = nt;
                        sc = n - (n >>> 2);
                    }
                } finally {
                    sizeCtl = sc;
                }
            }
        }
        else if (c <= sc || n >= MAXIMUM_CAPACITY)
            break;
        else if (tab == table) {
            int rs = resizeStamp(n);
            // 若正在扩容table，则帮助扩容
            // 否则开始新的扩容
            if (sc < 0) {
                Node<K,V>[] nt;
                if ((sc >>> RESIZE_STAMP_SHIFT) != rs || sc == rs + 1 ||
                        sc == rs + MAX_RESIZERS || (nt = nextTable) == null ||
                        transferIndex <= 0)
                    break;
                // transfer的线程数加一
                if (U.compareAndSwapInt(this, SIZECTL, sc, sc + 1))
                    transfer(tab, nt);
            }
            // 没有在初始化或扩容，则开始扩容
            else if (U.compareAndSwapInt(this, SIZECTL, sc,
                                             (rs << RESIZE_STAMP_SHIFT) + 2))
                transfer(tab, null);
        }
    }
}
```

