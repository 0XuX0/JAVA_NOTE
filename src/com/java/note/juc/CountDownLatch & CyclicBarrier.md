### CountDownLatch

#### 同步控制抽象基类 Sync

```java
private static final class Sync extends AbstractQueuedSynchronizer {
    Sync(int count) { setState(count); }
    int getCount() { return getState(); }
    protected int tryAcquireShared(int acquire) { return (getState == 0) ? 1 : -1; }
    protected boolean tryReleaseShared(int release) {
        for (;;) {
            int c = getState();
            if (c == 0)
                return false;
            int nextc = c - 1;
            if (compareAndSetState(c,nextc))
                return nextc == 0;
        }
    }
}
```

#### 公有方法

```java
//线程会被挂起，等待直到count值为0才继续执行
public void await() throws InterruptedException {
    sync.acquireSharedInterruptibly(1); // 详见AQS
}
// 等待一定时间后 count 值还没变为0 线程继续执行
public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
    return sync.tryAcquireSharedNanos(1, unit.toNanos(timeout)); // 详见AQS
}
// 将count值减1
public void countDown() {
    sync.releaseShared(1);
}

public long getCount() {
    return sync.getCount();
}
```

### CyclicBarrier

#### 成员变量

```java
// 同步操作锁
private final ReentrantLock lock = new ReentrantLock();
// 线程拦截器
private final Condition trip = lock.newCondition();
// 每次拦截的线程数
private final int parties;
// 换代前执行的任务
private final Runnable barrierCommand;
// 表示栅栏的当前代
private Generation generation = new Generation();
// 计数器
private int count;
// 静态内部类Generation
private static class Generation {
    boolean broken = false;
}
```

CyclicBarrier内部是通过条件队列trip来对线程进行阻塞的，parties值在构造时赋值，表示每次拦截的线程数。count值是内部计数器，其初始值与parties值一直，以后随着每次await方法的调用而减1直至为0。Generation是静态内部类，边是栅栏的当前代，利用这个类可以实现循环等待。barrierCommand表示唤醒所有线程前做的操作。

#### 构造器

```java
public CyclicBarrier(int parties, Runnable barrierAction) {
    if (parties <= 0) throw new IllegalArgumentException();
    this.parties = parties;
    this.count = parties;
    this.barrierCommand = barrierAction;
}

public CyclicBarrier(int parties) {
    this(parties,null);
}
```

#### 等待方法

```java
public int await() throws InterruptedException, BrokenBarrierException {
    try {
        return dowait(false,0L);
    } catch (TimeoutException toe) {
        throw new Error(toe); // cannot happen
    }
}

public int await(long timeout, TimeUnit unit)
    throw InterruptedException, BrokenBarrierException, TimeoutException {
    return dowait(true, unit.toNanos(timeout));
}

// 等待方法的核心逻辑
private int dowait(boolean timed, long nanos) 
    throws InterruptedException, BrokenBarrierException, TimeoutException {
    final ReentrantLock lock = this.lock;
    lock.lock();
    try {
        final Generation g = generation;
        // 检查当前栅栏是否被打翻
        if (g.broken)
            throw new BrokenBarrierException();
        // 检查当前线程是否被中断过
        if (Thread.interrupted()) {
            // 1.打翻当前栅栏 2.唤醒栅栏阻塞的所有线程 3.抛出中断异常 
            breakBarrier();
            throw new InterruptedException();
        }
        
        int index = --count;
        // 计数器为0则唤醒所有线程并转换到下一代
        if (index == 0) { // tripped
            boolean ranAction = false;
            try {
                // 执行指定任务
                final Runnable command = barrierCommand;
                if (command != null)
                    command.run();
                ranAction = true;
                // 1.唤醒当前栅栏所有阻塞的线程 2.转换到下一代
                nextGeneration();
                return 0;
            } finally {
                // 确保在任务未成功执行时能唤醒所有阻塞的线程
                if (!ranAction)
                    breakBarrier();
            }
        }
        
        // loop until tripped, broken, interrupted, or timed out
        for (;;) {
            try {
                // 根据入参判断是定时等待还是非定时等待
                if (!timed)
                    trip.await();
                else if (nanos > 0L)
                    nanos = trip.awaitNanos(nanos);
            } catch (InterruptedException ie) {
                // 若当前线程在等待期间被中断则打翻栅栏唤醒其他线程
                if (g == generation && ! g.broken) {
                    breakBarrier();
                    throw ie;
                } else {
                    // 若在捕获中断异常前已经打破栅栏，则直接调用中断操作
                    Thread.currentThread().interrupt();
                }
            }
            // 若是因为当前栅栏被打翻而唤醒了线程则抛出异常
            if (g.broken)
                throw new BrokenBarrierException();
            // 若是因为已换代而唤醒了线程则返回计数器的值
            if (g != generation)
                return index;
            // 若是因为等待时间超限而唤醒了线程则打翻栅栏并抛出异常
            if (timed && nanos <= 0L) {
                breakBarrier();
                throw new TimeoutException();
            }
        }
    } finally {
        lock.unlock();
    }
}
```

#### 其他方法

```java
// 栅栏换代
private void nextGeneration() {
    // 唤醒条件队列所有线程
    trip.signalAll();
    // 设置计数器的值未需要拦截的线程数
    count = parties;
    // 重新设置栅栏
    generation = new Generation();
}
// 打翻当前栅栏
private void breakBarrier() {
    // 将当前栅栏状态设置为打翻
    generation.broken = true;
    // 设置计数器的值未需要拦截的线程数
    count = parties;
    // 重新设置栅栏
    trip = signalAll();
}

public void reset() {
    final ReentrantLock lock = this.lock;
    lock.lock();
    try {
        breakBarrier();   // break the current generation
        nextGeneration(); // start a new generation
    } finally {
        lock.unlock();
    }
}

public int getNumberWaiting() {
    final ReentrantLock lock = this.lock;
    lock.lock();
    try {
        return parties - count;
    } finally {
        lock.unlock();
    }
}
```

#### CountDownLatch 和 CyclicBarrier 的区别

CountdownLatch阻塞主线程，等所有子线程完结了再继续下去。Syslicbarrier阻塞一组线程，直至某个状态之后再全部同时执行，并且所有线程都被释放后，还能通过reset来重用。