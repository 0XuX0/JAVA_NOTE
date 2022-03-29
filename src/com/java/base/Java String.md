## Java String

### 概念

String 是定义在 java.lang 包下的一个类，它不是基本数据类型。String的不可变的，JVM使用字符串池来存储所有的字符串对象。

### 创建String对象

+ new 关键字

  JVM在堆区创建字符串对象，可以调用intern()方法将该字符串对象存储在字符串池，如果字符串池已经有了同样值的字符串，则返回引用

+ 使用双引用直接创建

  JVM区字符串池查找有没有值相等的字符串，如果有，则返回字符串引用。否则创建一个新的字符串对象并存储在字符串池

### String 不可变的好处

+ String在多线程中使用是安全的
+ String可以用来存储数据密码
+ 可以在java运行时节省大量java堆空间。

### 面试题

```java
String s1 = new String("abc");
String s2 = new String("abc");
System.out.println(s1 == s2); // false
String s3 = "abc";
Stirng s4 = new String("abc");
s4 = s4.intern();
System.out.println(s3 == s4); // true

// Q：s1 和 s2创建几个字符串对象
// A：3 （字符串池一个 对内存有两个）
```

### String StringBuffer StringBuilder 的区别

+ 可变性

  String 的底层数据结构是 private final char value[] ，故其是不可变的，而StringBuffer，StringBuilder 都继承自AbstractStringBuilder类，其底层数据结构是没有final关键字修饰的，故其是可变的

+ 线程安全性

  String 中的对象是不可变的，天然线程安全；

  StringBuffer 中对方法加了同步锁或对调用的方法加了同步锁，线程安全；

  StringBuilder 是非线程安全的；

+ 使用场景

  1. 操作少量数据：String
  2. 单线程操作字符串缓冲区下大量数据：StringBuilder
  3. 多线程操作字符串缓冲区下大量数据：StringBuffer

### String 的 equals() 方法

String 中 equals 方法是被重写过的，因为 Object 的 equals 方法是比较对象的内存地址，而String的equals方法比较的是对象的值