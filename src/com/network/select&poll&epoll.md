### 概念解释

#### 用户空间与内核空间

操作系统都是采用虚拟存储器，其核心的内核，独立于普通的应用程序，可以访问受保护的内存空间，也有访问底层硬件设备的所有权限。为了保证用户进程不能直接操作内核，保证内核的安全，操作系统将虚拟空间划分为两部分，一部分为内核空间，一部分为用户空间。

#### 文件描述符 fd

文件描述符(File descriptor)是计算机科学中的一个术语，用于表述指向文件的引用的抽象化概念。

文件描述符在形式上是一个非负整数。实际上是一个索引值，指向内核为每一个进程所维护的该进程打开文件的记录表。当程序打开一个现有文件或者创建一个新文件时，内核向进程返回一个文件描述符。

#### 缓存IO

缓存I/O又被称作标准I/O。在Linux的缓存I/O机制中，操作系统会将I/O的数据缓存在文件系统的页缓存(page cache)中，数据会先被拷贝到操作系统内核的缓冲区中，然后才会从操作系统内核的缓冲区拷贝到应用程序的地址空间

### I/O多路复用

select、poll、epoll都是IO多路复用的机制。I/O多路复用就是通过一种机制，一个进程可以监视多个描述符，一旦某个描述符就绪(一般是读就绪或者写就绪)，能够通知程序进行相应的读写操作。

场景举例如下：

> 某教室有10名学生和1名老师，这些学生上课会不停地提问，所以一个老师处理不了这么多的问题。那么学校为每个学生都配一名老师，也就是这个教室目前有10名老师。此后，只要有新的转校生，那么就会为这个学生专门分配一个老师。如果把上述例子中的学生比作客户端，那么老师就是负责进行数据交换的服务端。该场景可以比作是多进程的方式。
>
> 后来有一天，来了一位能力超强的老师，这位老师回答问题非常迅速，并且可以应对所有问题。而这位老师采用的方式是学生提问前必须先举手，确认举手学生后再回答问题。该场景就是IO复用

### select

```c++
int select (int n, fd_set *readfds, fdset *writefds, fdset *exceptfds, struct timeval *timeout);
```

select函数监视的文件描述符分3类，分别是writefds、readfds和exceptfds。调用select函数会阻塞，直到有描述符就绪(有数据可读、可写、或者有except)，或者超时，函数返回。当select函数返回后，可以通过遍历fdset，来找到就绪的描述符。把fdset从内核空间拷贝到用户空间

![select过程](pic\select.png)

缺点：

1. 单个进程可监视的fd数量被限制，即能监听端口的大小有限。和系统内存大小相关，64位默认2048
2. 对内核的fd进行扫描是线性扫描，即采用轮询的方法，效率较低
   + 每次调用select，都需要把fd集合从用户态拷贝到内核态，开销在fd很多时会很大
   + 每次调用select，都需要在内核遍历传递进来的所有fd，开销在fd很多时会很大
3. 需要维护一个用来存放大量fd的数据结构，使得用户空间和内核空间在传递时复制开销大

### poll

```c++
int poll (struct pollfd * fds, unsigned int nfds, int timeout);
struct pollfd {
    int fd;
    short events;
    short revents;
}
```

poll的实现和select非常相似，只是poll使用pollfd结构，但是poll没有最大文件描述符数量的限制。和select函数一样，poll被调用后，需要轮询pollfd来获取就绪的描述符

> 从上面看，select和poll都需要在被调用之后，通过遍历文件描述符来获取已经就绪的socket。实际上同时连接的大量客户端在同一时刻可能只有很少的处于就绪状态，因此随着监视的描述符数量的增长，其效率会线性下降

### epoll

#### epoll 操作过程

```c++
int epoll_create(int size);
int epoll_ctl(int epfd, int op, int fd, struct epoll_event *event);
int epoll_wait(int epfd, struct epoll_event *events, int maxevents, int timeout);
```

1. epoll_create

   创建一个epoll的句柄，size参数告诉内核这个监听的数目一共多大。创建好epoll句柄后，它就会占用一个fd值，在使用完epoll后必须调用close()关闭，否则可能导致fd被耗尽

2. epoll_ctl

   函数是对指定描述符fd执行op操作

   - epfd：是epoll_create()的返回值
   - op：表示op操作，用三个宏表示：添加EPOLL_CTL_ADD 删除EPOLL_CTL_DEL 修改EPOLL_CTL_MOD
   - fd：需要监听的文件描述符
   - epoll_event：告诉内核需要监听什么事，结构定义如下

   ```c++
   struct epoll_event {
       __uint32_t events;  /* Epoll events */
       epoll_data_t data; /* User data variable */
   };
   //events可以是以下几个宏的集合：
   EPOLLIN ：表示对应的文件描述符可以读（包括对端SOCKET正常关闭）；
   EPOLLOUT：表示对应的文件描述符可以写；
   EPOLLPRI：表示对应的文件描述符有紧急的数据可读（这里应该表示有带外数据到来）；
   EPOLLERR：表示对应的文件描述符发生错误；
   EPOLLHUP：表示对应的文件描述符被挂断；
   EPOLLET： 将EPOLL设为边缘触发(Edge Triggered)模式，这是相对于水平触发(Level Triggered)来说的。
   EPOLLONESHOT：只监听一次事件，当监听完这次事件之后，如果还需要继续监听这个socket的话，需要再次把这个socket加入到EPOLL队列里
   ```

3. epoll_wait

   等待epfd上的io事件，最多返回maxevents个事件

   + events：从内核得到事件的集合
   + maxevents：集合events的大小，不能大于创建epoll_create()的size
   + timeout：超时时间

#### 工作模式

+ LT模式(level trigger)：当epoll_wait检测到描述符事件发生并将此事件通知应用程序，应用程序可以不立即处理该事件。只要这个fd还有数据可读，每次调用epoll_wait时，都会通知此事件。
+ ET模式(edge trigger)：当epoll_wait检测到描述符事件发生并将此事件通知应用程序，应用程序必须立即处理该事件。如果不处理，直到下次再有数据流入之前都不会有事件通知。

所以在ET模式下，read一个fd的时候一定要将其buffer读光，即一直读到read的返回值小于请求值，或者遇到EAGAIN错误。

epoll使用事件就绪通知方式，通过epoll_cli注册fd，一旦fd就绪，内核就会采用类似callback的回调机制来激活该fd，epoll_wait便可以收到通知

#### epoll 的优点：

1. 没有最大并发连接的限制，能打开的fd的上限远大于1024
2. 效率提升，不是轮询方式。只有活跃可用的FD才会调用callback函数，与连接总数无关
3. 内存拷贝，利用mmap()文件映射内存加速与内核空间的消息传递；即epoll使用mmap减少复制开销

#### epoll 如何避免 select 和 poll 的缺点

+ epoll_ctl函数中，每次注册新的事件到epoll句柄中时，会把所有的fd拷贝进内核，而不是在epoll_wait的时候重复拷贝。epoll保证了每个fd在整个过程中只会拷贝一次
+ 对于第二个缺点，epoll不像select或poll一样每次都把current轮流加入fd对应的设备等待队列中，而只在epoll_ctl时把current挂一遍并为每个fd指定一个回调函数，当设备就绪，唤醒等待队列上的等待者时就会调用这个回调函数，这个回调函数会把就绪的fd加入一个就绪链表。epoll_wait的工作实际上就是在这个就绪链表中查看有没有就绪的fd

#### Linux内核具体的 epoll机制实现思路

1. 某一进程调用epoll_create方法时，Linux内核会创建一个eventpoll结构体

   ```c++
   struct eventpoll {
       /*红黑树的根节点，这颗树中存储着所有添加到epoll中的需要监控的事件*/
       struct rb_root  rbr;
       /*双链表中则存放着将要通过epoll_wait返回给用户的满足条件的事件*/
       struct list_head rdlist;
   }
   ```

2. 调用epoll_ctl方法向epoll对象添加事件。这些事件会挂载到红黑树中，如此，可高效地识别出是否重复添加事件

3. 所有添加到epoll中的事件都会与设备(网卡)驱动建立回调关系，当相应的事件发生时会调用这个回调方法。该回调方法在内核中将发生的事件添加到rdlist双链表中。

4. 每一个事件对应epitem结构体，当调用epoll_wait检查是否有事件发生时，只需要检查eventpoll对象中的rdlist双链表中是否有epitem元素即可。如果rdlist不为空，则把发生的事件复制到用户态。

   ```c++
   struct epitem{
       struct rb_node  rbn;//红黑树节点
       struct list_head    rdllink;//双向链表节点
       struct epoll_filefd  ffd;  //事件句柄信息
       struct eventpoll *ep;    //指向其所属的eventpoll对象
       struct epoll_event event; //期待发生的事件类型
   }
   ```

   ![epoll](pic\epoll.png)

