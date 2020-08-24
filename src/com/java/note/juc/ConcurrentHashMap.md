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

