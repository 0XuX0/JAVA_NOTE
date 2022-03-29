CopyOnWriteArrayList

CopyOnWriteArrayList 是使用空间换时间的方式进行工作，适用于读多写少的场景。修改时才上锁。

成员变量

```java
final transient ReentrantLock lock = new ReentrantLock(); // 可重入锁
private transient volatile Object[] array; // 底层数组
```

get方法

```java
// get 方法直接从原数组中获取元素
public E get(int index) { return get(getArray(), index); }
private E get(Object[] a, int index) {
    return (E) a[index];
}
public E set(int index, E element) {
    // 加可重入锁
    final ReentrantLock lock = this.lock;
    lock.lock();
    try {
        Object[] elements = getArray();
        // 从老数组中获取对应位置的元素 oldValue
        E oldValue = get(elements, index);
        // 若oldValue与目标元素不一致
        if (oldValue != element) {
            int len = elements.length;
            // 复制数组到一个新数组中
            Object[] newElements = Arrays.copyOf(elements, len);
            // 在新数组中进行赋值
            newElements[index] = element;
            // 使用新数组替换老数组
            setArray(newElements);
        } else {
            setArray(elements);
        }
        return oldValue;
    } finally {
        // 解锁
        lock.unlock();
    }
}
```

add方法

```java
// 在数组最后一个位置添加元素
// 复制旧数组到一个长度为len + 1的新数组中，覆盖旧数组
public boolean add(E e) {
    final ReentrantLock lock = this.lock;
    lock.lock();
    try {
        Object[] elements = getArray();
        int len = elements.length;
        Object[] newElements = Arrays.copyOf(elements, len + 1);
        newElements[len] = e;
        setArray(newElements);
        return true;
    } finally {
        lock.unlock();
    }
}
// 在数组指定位置添加元素
// 复制旧数组到一个长度为len + 1的新数组中
//（0，index）复制到对应位置，（index，len）复制到（index+1，len+1）位置
public void add(int index, E element) {
    final ReentrantLock lock = this.lock;
    lock.lock();
    try {
        Object[] elements = getArray();
        int len = elements.length;
        if (index > len || index < 0)
            throw new IndexOutOfBoundsException("Index: "+index+
                                                ", Size: "+len);
        Object[] newElements;
        int numMoved = len - index;
        if (numMoved == 0)
            newElements = Arrays.copyOf(elements, len + 1);
        else {
            newElements = new Object[len + 1];
            System.arraycopy(elements, 0, newElements, 0, index);
            System.arraycopy(elements, index, newElements, index + 1,
                             numMoved);
        }
        newElements[index] = element;
        setArray(newElements);
    } finally {
        lock.unlock();
    }
}

public boolean addIfAbsent(E e) {
    Object[] snapshot = getArray();
    return indexOf(e, snapshot, 0, snapshot.length) > 0 ? false : addIfAbsent(e, snapshot);
}
private boolean addIfAbsent(E e, Object[] snapshot) {
    final ReentrantLock lock = this.lock;
    lock.lock();
    try {
        Object[] current = getArray();
        int len = current.length;
        // 此时和其他add操作发生了竞争冲突
        if (snapshot != current) {
            int common = Math.min(snapshot.length, len);
            // 遍历common长度，若已找到目标元素则返回false
            for (int i = 0; i < common; i++)
                if (current[i] != snapshot[i] && eq(e, current[i]))
                    return false;
            // 在（len - common）中查找目标元素，若找到则返回false
            if (indexOf(e, current, common, len) >= 0)
                return false;
        }
        // 未发生其他add操作冲突，直接在尾部添加元素，并覆盖旧数组
        Object[] newElements = Arrays.copyOf(current, len + 1);
        newElements[len] = e;
        setArray(newElements);
        return true;
    } finally {
        lock.unlock();
    }
}
```

remove方法

```java
// 移除指定位置的数组元素
// 复制旧数组到一个长度为len - 1的新数组中
//（0，index）复制到对应位置，（index + 1，len）复制到（index，len - 1）位置
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
            Object[] newElements = new Object[len - 1];
            System.arraycopy(elements, 0, newElements, 0, index);
            System.arraycopy(elements, index + 1, newElements, index,
                             numMoved);
            setArray(newElements);
        }
        return oldValue;
    } finally {
        lock.unlock();
    }
}
```

removeAll

```java
// 移除当前数组中所有和集合c元素相同的元素
public boolean removeAll(Collection<?> c) {
    if (c == null) throw new NullPointerException();
    final ReentrantLock lock = this.lock;
    lock.lock();
    try {
        Object[] elements = getArray();
        int len = elements.length;
        if (len != 0) {
            int newlen = 0;
            Object[] temp = new Object[len];
            // 遍历，将c中不含的元素复制到新数组中
            for (int i = 0; i < len; i++) {
                Object element = elements[i];
                if (!c.contains(element))
                    temp[newlen++] = element;
            }
            if (newlen != len) {
                // 新数组覆盖旧数组
                setArray(Arrays.copyOf(temp, newlen));
                return true;
            }
        }
        return false;
    } finally {
        lock.unlock();
    }
}
public boolean retainAll(Collection<?> c) {
    if (c == null) throw new NullPointerException();
    final ReentrantLock lock = this.lock;
    lock.lock();
    try {
        Object[] elements = getArray();
        int len = elements.length;
        if (len != 0) {
            // temp array holds those elements we know we want to keep
            int newlen = 0;
            Object[] temp = new Object[len];
            // 遍历，将c中含有的元素复制到新数组中
            for (int i = 0; i < len; ++i) {
                Object element = elements[i];
                if (c.contains(element))
                    temp[newlen++] = element;
            }
            if (newlen != len) {
                setArray(Arrays.copyOf(temp, newlen));
                return true;
            }
        }
        return false;
    } finally {
        lock.unlock();
    }
}
```

