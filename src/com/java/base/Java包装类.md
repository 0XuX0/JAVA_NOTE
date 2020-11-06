Java包装类

概念

Java中每种基本类型都有一个对应的包装类，每个包装类的对象可以封装一个相应的基本类型的数据，并提供了其他一些有用的方法。包装类对象一经创建，其内容不可改变。

+ byte -> Byte
+ short -> Short
+ int -> Integer
+ long -> Long
+ floaat -> Float
+ double -> Double
+ boolean -> Boolean
+ char -> Character

基本数据类型和包装类的区别

1. 定义不同。包装类属于对象
2. 声明和使用方式不同。包装类使用new初始化，有些集合类的定义不能使用基本数据类型
3. 初始值不同。包装类默认值为null
4. 存储方式和位置不同，从而性能不同。基本数据类型存储在栈中，包装类则分为引用和实例，引用在栈中，具体实例在堆中。

拆箱/装箱

基本类型和对应的包装类可以相互转换：

+ 由基本类型向对应的包装类转换称为装箱，例如把 int 包装成 Integer 类的对象：

  ```java
  Integer i = Integer.valueOf(1);  // 手动装箱
  Integer j = 1; // 自动装箱
  ```

+ 包装类向对应的基本类型转换称为拆箱，如果把 Integer 类的对象重新简化为 int。

  ```java
  Integer i0 = new Integer(1);
  int i1 = i0; // 自动拆箱
  int i2 = i0.intValue(); // 手动拆箱
  ```

面试注意点

1. 关于equals

   ```java
   double i0 = 0.1;
   Double i1 = new Double(0.1);
   Double i2 = new Double(0.1);
   System.out.println(i1.equals(i2)); // true 2个包装类比较，比较的是包装的基本数据类型的值
   // true 基本数据类型和包装类型比较时，会先把基本数据类型包装后再比较
   System.out.println(i1.equals(i0)); 
   
   // Integer equals 方法
   public boolean equals(Object obj) {
       if (obj instanceof Integer) {
           return value == ((Integer)obj).iniValue();
       }
       return false
   }
   ```

   包装类型的equals方法参数是Obeject类型，所以要先将基本数据类型转换成对应的包装类后才能比较

2. 关于 == 比较

   **对于基本数据类型，==号比较的是值，而对于包装类型，==比较的是2个对象的内存地址**

   ```java
   double i0 = 0.1;
   Double i1 = new Double(0.1);
   Double i2 = new Double(0.1);
   System.out.println(i1 == i2); // false new 出来的都是新的对象
   System.out.println(i1 == i0); // true 基本类型和包装类比较，会先把包装类拆箱
   
   Double i3 = Double.valueOf(0.1);
   Double i4 = Double.valueOf(0.1);
   System.out.println(i3 == i4); // false valueOf方法内部实际上也是new
   ```

   ```java
   System.out.println(Integer.valueOf(1) == Integer.valueOf(1)); // true;
   System.out.println(Integer.valueOf(999) == Integer.valueOf(999)); // false
   
   // Integer valueOf 方法
   public static Integer valueOf(int i) {
       if (i >= IntegerCache.low && i <= IntegerCache.high)
           return IntegerCache.cahce[i + (-IntegerCache.low)];
       return new Integer(i);
   }
   ```

   从源码可知，从-128到127之间的值都被缓存到cache中，叫做 **包装类的缓存**

   Java对部分经常使用的数据 被调用 **valueOf** 方法时，采用缓存技术(new 方法不会使用缓存)，在类第一次被加载时创建缓存和数据。当使用等值对象时直接从缓存中获取，从而提高程序执行性能。以下是常用的数据类型的缓存：

   + Integer类型有缓存 -128到127的对象。缓存上限可以通过配置jvm更改
   + Byte，Short，Long类型有缓存 -128 到 127
   + Character缓存 0 - 127
   + Boolean缓存 TRUE、FALSE

3. 包装类型运算会触发自动拆箱

练习

```java
Integer a = 1;
Integer b = 2;
Integer c = 3;
Integer d = 3;
Integer e = 321;
Integer f = 321;
Long g = 3L;
// 自动装箱实际上等同于 Integer.valueOf(1)
// 故使用到了包装类缓存池
System.out.println(c == d); // true
System.out.println(e == f); // false
// 包装类运算会自动拆箱
// 包装类和基本类型比较，会先把包装类型拆箱
System.out.println(c == (a + b)); // true
System.out.println(g == (a + b)); // true
// 包装类运算会自动拆箱
// 基本数据类型和包装类型比较，会先把基本数据类型转成包装型 3转成了Integer而不是Long
System.out.println(c.equals((a + b))); // true
System.out.println(g.equals((a + b))); // false
```

