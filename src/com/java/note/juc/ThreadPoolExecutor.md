## ThreadPoolExecutor

### 为什么要使用线程池

+ 创建/销毁线程需要消耗系统资源，线程池可以复用已创建的线程
+ 控制并发的数量。
+ 可以对线程做统一管理

### 构造函数

```java
public ThreadPoolExecutor(int corePoolSize, 
                          int maximumPoolSize, 
                          long keepAliveTime, 
                          TimeUnit unit, 
                          BlockingQueue<Runnable> workQueue) {
    this(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, Executors.defaultThreadFactory(), defaultHandler);
}

public ThreadPoolExecutor(int corePoolSize,
                         int maximumPoolSize,
                         long keepAliveTime,
                         TimeUnit unit,
                         BlockingQueue<Runnable> workQueue,
                         ThreadFactory threadFactory) {
    this(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, defaultHandler);
}

public ThreadPoolExecutor(int corePoolSize,
                         int maximumPoolSize,
                         long keepAliveTime,
                         TimeUnit unit,
                         BlockingQueue<Runnable> workQueue,
                         RejectedExecutionHandler handler) {
    this(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue,
        Executors,defaultThreadFactory(), handler);
}

public ThreadPoolExecutor(int corePoolSize,
                         int maximumPoolSize,
                         long keepAliveTime,
                         TimeUnit unit,
                         BlockingQueue<Runnable> workQueue,
                         ThreadFactory threadFactory,
                         RejectedExecutionHandler handler) {
    if (corePoolSize < 0 ||
       maximumPoolSize <= 0 ||
       maximumPoolSize < corePoolSize ||
       keepAliveTime < 0) 
        throw new IllegalArgumentException();
    if (workQueue == null || threadFactory == null || handler == null)
        throw new NullPointerException();
    this.acc = System.getSecurityManager() == null ? null : AccessController.getContext();
    this.corePoolSize = corePoolSize;
    this.maximumPoolSize = maximumPoolSize;
    this.workQueue = workQueue;
    this.keepAliveTime = unit.toNanos(keepAliveTime);
    this.threadFactory = threadFactory;
    this.handler = handler;
}
```

+ int corePoolSize：该线程池中核心线程数最大值

  核心线程：线程池中有两类线程，核心线程和非核心线程。核心线程默认情况下会一直存在于线程池中，即使这个核心线程什么都不干，而非核心线程如果长时间的闲置，就会被销毁。

+ int maximumPoolSize：该线程池中线程总数最大值

  该值等于核心线程数量 + 非核心线程数量

+ long keepAliveTime：非核心线程闲置超时时长

  非核心线程如果处于闲置状态超过该值，就会被销毁。如果设置 allowCoreThreadTimeOut(true)，则会作用于核心线程

+ TimeUnit unit：keppAliveTime 的单位

+ BlockingQueue workQueue：阻塞队列，维护者等待执行的 Runnable 任务对象。

  常用的阻塞队列有：

  1. LinkedBlockingQueue

     链式阻塞队列，底层数据结构是链表，默认大小是 Integer.MAX_VALUE，也可以指定大小。

  2. ArrayBlockingQueue

     数组阻塞队列，底层数据结构是数组，需要指定队列的大小。

  3. SynchronousQueue

     同步队列，内部容量为0，每个put操作必须等待一个take操作。

  4. DelayQueue

     延迟队列，该队列中的元素只有当其指定的延迟时间到了，才能够从队列中获取到该元素。

+ ThreadFacrory threadFactory

  创建线程的工厂，用于批量创建线程，统一在创建线程时设置一些参数，如是否守护线程、线程的优先级等。如不指定，会新建一个默认的线程工厂。

+ RejectedExecutionHandler handler

  拒绝处理策略：

  1. ThreadPoolExecutor.AbortPolicy：默认拒绝处理策略，丢弃任务并抛出RejectedExecutionException异常
  2. ThreadPoolExecotor.DiscardPolicy：丢弃新来的任务，但是不抛出异常。
  3. ThreadPoolExecutor.DiscardOldestPolicy：丢弃队列头部(最旧)的任务，然后重新尝试执行程序。
  4. ThreadPoolExecutor.CallerRunsPolicy：由调用线程处理该任务。

### 线程池状态转换

```java
// runState 
private static final int COUNT_BITS = Integer.SIZE - 3;
private static final int RUNNING    = -1 << COUNT_BITS;
private static final int SHUTDOWN   =  0 << COUNT_BITS;
private static final int STOP       =  1 << COUNT_BITS;
private static final int TIDYING    =  2 << COUNT_BITS;
private static final int TERMINATED =  3 << COUNT_BITS;

```

### 线程池任务处理流程

```java
public void execute(Runnable command) {
    if (command == null) 
        throw new NullPointerExeception();
    // 1.当前线程数小于corePoolSize,则调用addWorker创建核心线程执行任务
    int c = ctl.get();
    if (workerConutOf(c) < corePoolSize) {
        if (addWorker(command, true))
            return;
        c = ctl.get();
    }
    // 2.如果不小于corePoolSize，则将任务添加到workQueue队列。
    if (isRunning(c) && workQueue.offer(command)) {
        int recheck = ctl.get();
        // 2.1 二次检查状态，如果isRunning返回false(状态检查)，则remove这个任务，然后执行拒绝策略。
        if (! isRunning(recheck) && remove(command))
            reject(command);
        // 2.2 线程池处于running状态，但是没有线程，则创建线程
        else if (workerCountOf(recheck) == 0)
            addWorker(null, false);
    }
    // 3.如果放入workQueue失败，则创建非核心线程执行任务，
    // 如果这时创建非核心线程失败(当前线程总数不小于maximumPoolSize时)，就会执行拒绝策略。
    else if (!addWorker(command, false))
        reject(command);
}
```

处理流程总结：

1. 线程总数量 < corePoolSize，无论线程是否空闲，都会新建一个核心线程执行任务（让核心线程数量快速达到corePoolSize）
2. 线程总数量 >= corePoolSIze时，新来的线程任务会进入任务队列中等待，然后空闲的核心线程会依次去缓存队列中取任务来执行（体现了线程复用）
3. 当缓存队列满了，说明此时任务已经很多，会创建非核心线程去执行这个任务
4. 缓存队列满了，且总线程数达到了 maximumPoolSize，则会采取拒绝策略进行处理

### 线程池如何做到线程复用的？

一般情况下，一个线程在创建的时候会指定一个线程任务，当执行完这个线程任务之后，线程自动销毁。但是线程池却可以复用线程，即一个线程执行完线程任务后不销毁，继续执行另外的线程任务。ThreadPoolExecutor 在创建线程时，会将线程封装成工作线程 worker，并放入工作线程组中，然后这个 worker 反复从阻塞队列中拿任务去执行。

```java
private boolean addWorker(Runnable firstTask, boolean core) {
    // （1）循环CAS操作，将线程池中的线程数+1
    retry:
    for (;;) {
        int c = ctl.get();
        int rs = runStateOf(c);
        
        // Check if queue empty only if necessary.
        if (rs >= SHUTDOWN &&
           ! (rs == SHUTDOWN &&
             firstTask == null &&
             ! workQueue.isEmpty()))
            return false;
        
        for(;;) {
            int wc = workerCountOf(c);
            if (wc >= CAPACITY ||
               wc >= (core ? corePoolSize : maximumPoolSize))
                // 如果core是ture,证明需要创建的线程为核心线程，则先判断当前线程是否大于核心线程
                // 如果core是false,证明需要创建的是非核心线程，则先判断当前线程数是否大于总线程数
                // 如果不小于，则返回false
                return false;
            if (compareAndIncrementWorkerCount(c))
                break retry;
            c = ctl.get();
            // 如果线程池状态发生了变化回到retry外层循环
            if (runStateOf(c) != rs)
                continue retry;
        }
    }
    //（2）新建线程，并加入到线程池workers中
    boolean workerStarted = false;
    boolean workerAdded = false;
    Worker w = null;
    try {
        w = new Worker(firstTask);
        final Thread t = w.thread;
        if (t != null) {
            // 对workers操作要加锁
            final ReentrantLock mainLock = this.mainLock;
            mainLock.lock();
            try {
                int rs = runStateOf(ctl.get());
                // 判断线程池状态
                // 1.线程池处于运行中
                // 2.SHUTDOWN时，添加空线程
                if (rs < SHUTDOWN ||
                   (rs == SHUTDOWN && firstTask == null)) {
                    // 判断添加的任务状态，如果已开始抛出异常
                    if (t.isAlive())
                        throw new IllegalThreadStateException();
                    // 将新建的线程加入到线程池中
                    workers.add(w);
                    int s = workers.size();
                    if (s > largestPoolSize)
                        largestPoolSize = s;
                    workerAdded = true;
                }
            } finally {
                mainLock.unlock();
            }
            // 线程添加线程池成功，开启新创建的线程
            if (workerAdded) {
                t.start();
                workerStarted = true;
            }
        }
    } finally {
        if (! workerStarted)
            addWorkerFailed(w);
    }
    return workerStarted;
}
```

Worker类

```java
private final class Worker extends AbstractQueuedSynchronizer implements Runnable {
    // 线程池中真正运行的线程，通过我们指定的线程工厂创建而来
    final Thread thread;
    // 线程包装的任务，thread 在 run 时主要调用了该任务的run方法
    Runnable firstTask;
    // 记录当前线程完成的任务数量
    volatile long completedTasks;
    Worker(Runnable firstTask) {
        setState(-1);
        this.firstTask = firstTask;
        this.thread = getThreadFactory().newThread(this);
    }
    
    public void run() {
        runWorker(this);
    }
    // 其余代码略...
}

// Worker.runWorker 方法
final void runWorker(Worker w) {
    Thread wt = Thread.currentThread();
    Runnable task = w.firstTask;
    w.firstTask = null;
    // 1.线程启动之后，通过unlock方法释放锁
    w.unlock();
    boolean completedAbruptly = true;
    try {
        // 2.Worker执行firstTask或从workQueue中获取任务，如果getTask方法不返回null,循环不退出
        while (task != null || (task = getTask()) != null) {
            // 2.1进行加锁操作，保证thread不被其他线程中断（除非线程池被中断）
            w.lock();
            // 2.2检查线程池状态，倘若线程池处于中断状态，当前线程将中断。
            if ((runStateAtLeast(ctl.get(), STOP) ||
                (Thread.interrupted() &&
                runStateAtLeast(ctl.get(), STOP))) &&
               !wt.isInterrupted())
                wt.interrupted();
            try {
                // 2.3执行beforeExecute 
                beforeExecute(wt, task);
                Throwable thrown = null;
                try {
                    // 2.4执行任务
                    task.run();
                } catch (RuntimeException x) {
                    thrown = x; throw x;
                } catch (Error x) {
                    thrown = x; throw x;
                } catch (Throwable x) {
                    thrown = x; throw new Error(x);
                } finally {
                    // 2.5执行afterExecute方法
                    afterExecute(task, thrown);
                }
            } finally {
                task = null;
                w.completedTasks++;
                // 2.6解锁操作
                w.unlock();
            }
        }
        completedAbruptly = false;
    } finally {
        processWorkerExit(w, completedAbruptly);
    }
}
```

首先去执行创建这个worker时就有的任务，当执行完这个任务后，worker的生命周期并没有结束，在while循环中，worker会不断地调用 getTask方法从阻塞队列中获取任务然后调用 task.run() 执行任务，从而达到复用线程的目的。只要 getTask 方法不返回 null，此线程就不会退出

```java
// Worker.getTask 方法源码
private Runnable getTask() {
    boolean timedOut = false;
    for(;;) {
        int c = ctl.get();
        int rs = runStateOf(c);
        if (rs >= SHUTDOWN && (rs >= STOP || workQueue.isEmpty())) {
            decrementWorkerCount();
            return null;
        }
        
        int wc = workerCountOf(c);
        // 1.allowCoreThreadTimeOut变量默认是false,核心线程即使空闲也不会被销毁
        // 如果为true,核心线程在keepAliveTime内仍空闲则会被销毁
        boolean timed = allowCoreThreadTimeOut || wc > corePoolSize;
        // 2.如果运行线程数超过了最大线程数，但是缓存队列已经空了，这时递减worker数量。
        // 如果有设置允许线程超时或者线程数量超过了核心线程数量，
        // 并且线程在规定时间内均未poll到任务且队列为空则递减worker数量
        if ((wc > maximumPoolSize || (timed && timedOut))
           && (wc > 1 || workQueue.isEmpty())) {
            if (compareAndDecrementWorkerCount(c))
                return null;
            continue;
        }
        
        try {
            // 3.如果timed为true,则会调用workQueue的poll方法获取任务.
            // 超时时间是keepAliveTime。如果超过keepAliveTime时长，
            // poll返回了null，上边提到的while循序就会退出，线程也就执行完了。
            // 如果timed为false（allowCoreThreadTimeOut为false
            // 且wc > corePoolSize为false），则会调用workQueue的take方法阻塞在当前。
            // 队列中有任务加入时，线程被唤醒，take方法返回任务，并执行。
            Runnable r = timed ?
                workQueue.poll(keepAliveTime, TimeUnit.NANOSECONDS) : workQueue.take();
            if (r != null)
                return r;
            timedOut = true;
        } catch (InterruptedException retry) {
            timedOut = false;
        }
    }
}
```

核心线程会一直卡在 workQueue.take 方法，被阻塞并挂起，不会占用CPU资源，直到拿到 Runnable 然后返回（如果 **allowCoreThreadTimeOut** 设置为true，那么核心线程就会去调用 poll 方法，若 poll 返回 null， 所以核心线程满足超时条件也会被销毁）。

非核心线程会调用 workQueue.poll 方法，如果超时还没拿到，下一次循环判断 compareAndDecrementWorkerCount 就会返回 null，Worker对象的 run() 方法循环体的判断为 null。

### 四种常见的线程池

+ newCachedThreadPool

  ```java
  public static ExecutorService newCachedThreadPool() {
      return new ThreadPoolExecutor(0, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS, 
                                   new SynchronousQueue<Runnable>());
  }
  ```

  运行流程如下：

  1. 提交任务进线程池
  2. 由于corePoolSize为0，不创建核心线程，线程池最大为Integer.MAX_VALUE。
  3. 尝试将任务添加到SynchronousQueue队列
  4. 如果SynchronousQueue入队成功，等待被当前运行的线程空闲后拉取执行。如果当前没有空闲线程，那么就创建一个非核心线程，然后从队列中拉取任务执行
  5. 如果SynchronousQueue已有任务在等待，入队操作将会阻塞。

  > 当需要执行很多短时间的任务时，CacheThreadPool的线程复用率比较高，会显著**提高性能**。而且线程60s后会回收，意味着即使没有任务进来，CacheThreadPool并不会占用很多资源

+ newFixedThreadPool

  ```java
  public static ExecutorService newFixedThreadPool(int nThreads) {
      return new ThreadPoolExecutor(nThreads, nThreads, 0L, TimeUnit.MILLISECONDS,
                                   new LinkedBlockingQueue<Runnable>());
  }
  ```

  核心线程数量和总线程数量相等，都是传入的参数nThreads，所以只能创建核心线程。因为LinkedBlockingQueue的默认大小是Integer.MAX_VALUE，故如果核心线程空闲，则交给核心线程处理；如果核心线程不空闲，则入队等待，直到核心线程空闲。

  与CachedThreadPool的区别：

  1. FixedThreadPool只会创建核心线程，而CachedThreadPool只会创建非核心线程
  2. 在getTask()方法，如果队列里没有任务可取，线程会一直阻塞在 LinkedBlockingQueue.take()，线程不会回收，CachedThreadPool会在60s后收回
  3. 没有任务的情况下，FixedThreadPool占用资源更多
  4. 都不会触发拒绝策略：FixedThreadPool是因为阻塞队列很大，CachedThreadPool是因为线程池很大

+ newSingleThreadExecutor

  ```java
  public static ExecutorService new SingleThreadExecutor() {
      return new FinalizableDelegatedExecutorService(1,1,0L,
                                                     TimeUnit.MILLISECONDS,
                                                    new LinkedBlockingQueue<Runnable>());
  }
  ```

  有且仅有一个核心线程，使用了LinkedBlockingQueue，故不会创建非核心线程。所有任务按照先来先执行，存储在任务队列里等待执行。

+ newScheduledThreadPool

  ```java
  public static ScheduledExecutorService newScheduledThreadPool(int corePoolSize) {
      return new ScheduledThreadPoolExecutor(corePoolSize);
  }
  public ScheduledThreadPoolExecutor(int corePoolSize) {
      super(corePoolSize, Integer.MAX_VALUE,
           DEFAULT_KEEPALIVE_MILLIS, MILLISECONDS,
           new DelayedWorkQueue());
  }
  ```

  创建一个定长线程池，支持定时及周期性任务执行

