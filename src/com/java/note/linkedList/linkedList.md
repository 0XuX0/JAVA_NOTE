### LinkedList
####概述
LinkedList 是用链表结构存储数据的，很适合数据的动态插入和删除，随机访问和遍历速度  
比较慢。另外，他还提供了 List 接口中没有定义的方法，专门用于操作表头和表尾元素，  
可以当作堆栈、队列和双向队列使用。

####继承关系 
![alt 继承关系图](Linkedlist.png)
>LinkedList 继承自 AbstractSequentialList，实现了 List Serializable Cloneable Deque 接口  

```
linkFirst(E e);//插入新节点到头部
linkLast(E e);//插入新节点到尾部
linkBefore(E e, Node<E> succ);//在指定节点前插入元素，指定节点不为null
unlinkFirst(Node<E> f);//删除头节点并返回该节点上的数据
unlinkLast(Node<E> l);//删除尾节点并返回该节点上的数据
unlink(Node<E> x);//删除某个指定节点
node(int index);//获取指定位置的节点，若index<size的一半则从头开始遍历反之从尾部倒着遍历
```
```
//queue operation
peek();//返回队首元素(不删除) if null return null
element();//返回队首元素(不删除) if null throw NoSuchElementException
poll();//返回队首元素并删除 if null return null
remove();//返回队首元素并删除 if null throw NoSuchElementException
offer(E e);//队尾添加元素
```
```
//Deque operation
offerFirst(E e);//在队首插入元素
offerLast(E e);//在队尾插入元素
peekFirst();//返回队首元素(不删除)
peekLast();//返回队尾元素(不删除)
pollFirst();//返回队首元素并删除
pollLast();//返回队尾元素并删除
```
```
Iterator<E> descendingIterator();//倒序迭代器
```