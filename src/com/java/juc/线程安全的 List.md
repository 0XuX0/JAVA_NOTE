###  线程安全的 List

#### Vector

Vector 使用了 synchronized 方法，同步方法加锁是 this 对象，无法控制锁定的对象。

```java
public synchronized E get(int index) {
    if (index >= elementCount)
        throw new ArrayIndexOutOfBoundsException(index);
    return elementData(index);
}
public synchronized E set(int index, E element) {
    if (index >= elementCount)
        throw new ArrayIndexOutOfBoundsException(index);
    E oldValue = elementData(index);
    elementData[index] = element;
    return oldValue;
}
public synchronized boolean add(E e) {
    modcount++;
    ensureCapacityHelper(elementCount + 1);
    elementData[elementCount++] = e;
    return true;
}
public synchronized E remove(int index) {
    modCount++;
    if (index >= elementCount)
        throw new ArrayIndexOutOfBoundsException(index);
    E oldValue = elementData(index);
    int numMoved = elementCount - index - 1;
    if (numMoved > 0)
         System.arraycopy(elementData, index+1, elementData, index, numMoved);
    elementData[--elementCount] = null;
    return oldValue;
}
```



#### Collections.SynchronizedList

SynchronizedList 的同步是使用 synchronized 代码块对 mutex 对象加锁，mutex 对象可通过构造函数传入，我们可以指定锁定的对象

```java
public E get(int index) { synchronized (mutex) {return list.get(index);} }
public E set(int index, E element) { synchronized (mutex) {return list.set(index, element);} }
public void add(int index, E element) { synchronized (mutex) {list.add(index, element);} }
public E remove(int index) { 
    synchronized (mutex) {
        return list.remove(index);
    } 
}
```

#### CopyOnWriteArrayList

CopyOnWriteArrayList 实现了 List 接口，内部持有一个 ReentrantLock 用于保证多线程并发场景下线程修改时，只复制一份副本内容，其底层实现是 volatile transient 声明的数组，其写时复制出一个新的数组，完成插入、修改或者移除操作后将数组赋值给新数组。其应用场景主要是读多写少的场景（白名单）

```java
// CopyOnWriteArrayList 底层实现
private transient volatile Object[] array;

public E get(int index) { return get(getArray(), index); }
private E get(Object[] a, int index) {
    return (E) a[index];
}
public E set(int index, E element) {
    final ReentrantLock lock = this.lock;
    lock.lock();
    try {
        Object[] elements = getArray();
        E oldValue = get(elements, index);
        
        if (oldValue != element) {
            int len = elements.length;
            // 复制一个相同长度的新数组
            Object[] newElements = Arrays.copyOf(elements, len);
            newElements[index] = element;
            setArray(newElements);
        } else {
            setArray(elements);
        }
        return oldValue;
    } finally {
        lock.unlock();
    }
}
public void add(int index, E element) {
    final ReentrantLock lock = this.lock;
    lock.lock();
    try {
        Object[] elements = getArray();
        int len = elements.length;
        if (index > len || index < 0)
            throw new IndexOutOfBoundsException("Index: "+index+", Size: "+len);
        Object[] newElements;
        int numMoved = len - index;
        if (numMoved == 0)
            newElements = Arrays.copyOf(elements, len + 1);
        else {
            // 原数组内容复制到新数组中，空出index位置
            newElements = new Object[len + 1];
            System.arraycopy(elements, 0, newElements, 0, index);
            System.arraycopy(elements, index, newElements, index + 1, numMoved);
        }
        newElements[index] = element;
        setArray(newElements);
    } finally {
        lock.unlock();
    }
}
public E remove(int index) {
    final ReentrantLock lock = this.lock;
    lock.lock();
    try {
        Object[] elements = getArray();
        int len = elements.length;
        E oldValue = get(elements, index);
        int numMoved = len - index - 1;
        if (numMoved == 0)
            setArray(Arrays.copyOf(elements, len - 1));
        else {
            // 原数组内容复制到新数组中，且往前移动覆盖index位置的值
            Object[] newElements = new Object[len - 1];
            System.arraycopy(elements, 0, newElements, 0, index);
            System.arraycopy(elements, index + 1, newElements, index, numMoved);
            setArray(newElements);
        }
        return oldValue;
    } finally {
        lock.unlock();
    }
}
```

> 写入时复制（CopyOnWrite，简称COW）思想：若有多个调用者同时要求相同的资源，他们会共同获取相同的指针指向相同的资源，直到某个调用者试图修改资源内容时，系统才会真正复制一份专用副本给该调用者，而其他调用者所见的最初的资源仍然不变。

