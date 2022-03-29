ReentrantReadWriteLock

成员变量

```java
private final ReentrantReadWriteLock.ReadLock readerLock;
private final ReentrantReadWriteLock.WriteLock writerLock;
final Sync sync;
```

同步控制抽象基类

```java
abstract static class Sync extends AbstractQueuedSynchronizer {
    // int 类型 state 变量拆分为高16位，代表共享模式；低16位，代表独占模式
    static final int SHARED_SHIFT = 16;
    static final int SHARED_UNIT = (1 << SHARED_SHIFT);
    static final int MAX_COUNT = (1 << SHARED_SHIFT) - 1;
    static final int EXCLUSIVE_MASK = (1 << SHARED_SHIFT) - 1;
    
    static int sharedCount(int c) { return c >>> SHARED_SHIFT; } // 返回共享锁的获取次数，左移16位
    static int exclusiveCount(int c) { return c & EXCLUSIVE_MASK; } // 返回独占锁的重入次数，c执行与操作
    
    // 每个线程读持有的读锁数量
    static final class HoldCounter {
        int count = 0;
        final long tid = getThreadId(Thread.currentThread());
    }
    static final class ThreadLocalHoldCounter extends ThreadLocal<HoldCounter> {
        public HoldCounter initialValue() {
            return new HoldCounter();
        }
    }
    private transient ThreadLocalHoldCounter readHolds; // 记录当前线程可重入读锁的数量
    private transient HoldCounter cachedHoldCounter; // 记录最后一个线程获取读锁的数量
    private transient Thread firstReader = null; // 记录第一个获取读锁的线程
    private transient int firstReaderHoldCount; // 记录第一个线程获取读锁的次数
    
    abstract boolean readerShouldBlock();
    abstract boolean writerShouldBlock();
    
    protected final boolean tryRelease(int releases) {
        if (!isHeldExclusively())
            throw new IllegalMonitorStateException();
        int nextc = getState() - releases;
        boolean free = exclusiveCount(nextc) == 0;
        if (free)
            setExclusiveOwnerThread(null);
        setState(nextc);
        return free;
    }
    
    protected final boolean tryAcquire(int acquires) {
        Thread current = Thread.currentThread();
        int c = getState();
        int w = exclusiveCount(c);
        if (c != 0) {
            if (w == 0 || current != getExclusiveOwnerThread())
                return false;
            if (w + exclusiveCount(acquires) > MAX_COUNT)
                throw new Error("Maximum lock count exceeded");
            setState(c + acquires);
            return true;
        }
        if (writerShouldBlock() || 
           !compareAndSetState(c, c + acquires))
            return false;
        setExclusiveOwnerThread(current);
        return true;
    }
    
     protected final boolean tryReleaseShared(int unused) {
         Thread current = Thread.currentThread();
         if (firstReader == current) {
             if (firstReaderHoldCount == 1)
                 firstReader = null;
             else 
                 firstReaderHoldCount--;
         } else {
             HoldCounter rh = cachedHoldCounter;
             if (rh == null || rh.tid != getThreadId(current))
                 rh = readHolds.get();
             int count = rh.count;
             if (count <= 1) {
                 readHolds.remove();
                 if (count <= 0)
                     throw unmatchedUnlockException();
             }
             --rh.count;
         }
         for(;;) {
             int c = getState();
             int nextc = c - SHARED_UNIT;
             if(compareAndSetState(c, nextc))
                 return nextc == 0;
         }
     }
    
    protected final int tryAcquireShared(int unused) {
        Thread current = Thread.currentThread();
        int c = getState();
        // 其他线程持有写锁，返回-1
        if (exclusiveCount(c) != 0 &&
           getExclusiveOwnerThread() != current)
            return -1;
        int r = sharedCount(c);
        // 判断获取读锁的线程是否应该被挂起以及尝试获取读锁是否成功
        if (!readerShouldBlock() &&
           r < MAX_COUNT &&
           compareAndSetState(c, c + SHARED_UNIT)) {
            if (r == 0) {
                firstReader = current;
                firstReaderHoldCount = 1;
            } else if (firstReader == current) {
                firstReaderHoldCount++;
            } else {
                HoldCounter rh = cachedHoldCounter;
                if (rh == null || rh.tid != getThreadId(current))
                    cachedHoldCounter = rh = readHolds.get();
                else if (rh.count == 0)
                    readHolds.set(rh);
                rh.count++;
            }
            return 1;
        }
        return fullTryAcquireShared(current);
    }
   
    final int fullTryAcquireShared(Thread current) {
        HoldCounter rh = null;
        for (;;) {
            int c = getState();
            if (exclusiveCount(c) != 0) {
                if (getExclusiveOwnerThread() != current)
                    return -1;
            } else if (readerShouldBlock()) {
                if (firstReader == current) {
                } else {
                    if (rh == null) {
                        rh = cachedHoldCounter;
                        if (rh == null || rh.tid != getThreadId(current)) {
                            rh = readHolds.get();
                            if (rh.count == 0)
                                readHolds.remove();
                        }
                    }
                    if (rh.count == 0)
                        return -1;
                }
            }
            if (sharedCount(c) == MAX_COUNT)
                throw new Error("Maximum lock count exceeded");
            if (compareAndSetState(c, c + SHARED_UNIT)) {
                if (sharedCount(c) == 0) {
                    firstReader = current;
                    firstReaderHoldCount = 1;
                } else if (firstReader == current) {
                    firstReaderHoldCount++;
                } else {
                    if (rh == null)
                        rh = cachedHoldCounter;
                    if (rh == null || rh.tid != getThreadId(current))
                        rh = readHolds.get();
                    else if (rh.count == 0)
                        readHolds.set(rh);
                    rh.count++;
                    cachedHoldCounter = rh; // cache for release
                }
                return 1;
            }
        }
    }
    
    final boolean tryWriteLock() {
        Thread current = Thread.currentThread();
        int c = getState();
        if (c != 0) {
            int w = exclusiveCount(c);
            if (w == 0 || current != getExclusiveOwnerThread())
                return false;
            if (w == MAX_COUNT)
                throw new Error("Maximum lock count exceeded");
        }
        if (!compareAndSetState(c, c + 1))
            return false;
        setExclusiveOwnerThread(current);
        return true;
    }
    
    final boolean tryReadLock() {
        Thread current = Thread.currentThread();
        for(;;) {
            int c = getState();
            if (exclusiveCount(c) != 0 &&
               getExclusiveOwnerThread() != current)
                return false;
            int r = sharedCount(c);
            if (r == MAX_COUNT)
                throw new Error("Maximum lock count exceeded");
            if (compareAndSetState(c, c + SHARED_UNIT)) {
                if (r == 0) {
                    firstReader = current;
                    firstReaderHoldCount = 1;
                } else if (firstReader == current) {
                    firstReaderHoldCount++;
                } else {
                    HoldCounter rh = cachedHoldCounter;
                    if (rh == null || rh.tid != getThreadId(current))
                        cachedHoldCounter = rh = readHolds.get();
                    else if (rh.count == 0)
                        readHolds.set(rh);
                    rh.count++;
                }
                return true;
            }
        }
    }
    
}
```

非公平同步块

```java
static final class NonfairSync extends Sync {
    final boolean writerShouldBlock() { return false; }
    final boolean readerShouldBlock() {
        return apparentlyFirstQueuedIsExclusive();
    }
}
```

公平同步块

```java
static final class FairSync extends Sync {
    final boolean writerShouldBlock() {
        return hasQueuedPredecessors();
    }
    final boolean readerShouldBlock() {
        return hasQueuedPredecessors();
    }
}
```

读锁内部类

```java
public static class ReadLock implements Lock, java.io.Serializable {
    private final Sync sync;
    protected ReadLock(ReentrantReadWriteLock lock) { sync = lock.sync; }
    public void lock() { sync.acquireShared(1); }
    public void lockInterruptibly() throws InterruptedException {
        sync.acquireSharedInterruptibly(1);
    }
    public boolean tryLock() { return sync.tryReadLock(); }
    public boolean tryLock(long timeout, TimeUnit unit) throws InterruptedException {
        return sync.tryAcquireSharedNanos(1, unit.toNanos(timeout));
    }
    public void unlock() { sync.releaseShared(1); }
}
```

写锁内部类

```java
public static class WriteLock implements Lock, java.io.Serializable {
    private final Sync sync;
    protected WriteLock(ReentrantReadWriteLock lock) { sync = lock.sync; }
    public void lock() { sync.acquire(1); }
    public void lockInterruptibly() throws InterruptedException {
        sync.acquireInterruptibly(1);
    }
    public boolean tryLock( ) { return sync.tryWriteLock(); }
    public boolean tryLock(long timeout, TimeUnit unit) throws InterruptedException {
        return sync.tryAcquireNanos(1, unit.toNanos(timeout));
    }
    public void unlock() { sync.release(1); }
}
```

