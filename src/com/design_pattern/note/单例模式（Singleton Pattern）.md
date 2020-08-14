## 单例模式（Singleton Pattern）

单例模式是指确保一个类在任何情况下都绝对只有一个实例，并提供一个全局访问点。Spring中IOC容器ApplicationContext本身就是典型的饿汉式单例模式。

### 饿汉式单利模式

饿汉式单例模式在类加载的时候就立即初始化，并且创建单例对象。它绝对线程安全，在线程还没出现前就实例化了，故不存在访问安全问题。

+ 优点：没有加任何锁、执行效率比较高，用户体验比懒汉式单例模式好。
+ 缺点：类加载的时候就初始化，不管用与不用都占着空间，浪费内存。

```java
public class HungrySingleton {
    private static final HungrySingleton hungrySingleton = new HungrySingleton();
    private HungrySingleton();
    public static HungrySingleton getInstance() {
        return hungrySingleton;
    }
}
```

### 懒汉式单例模式

懒汉式单例模式的特点是：被外部类调用的时候内部类才会加载。

```java
public class LazySimpleSingleton {
    private static LazySimpleSingleton lazy = null;
    private LazySimpleSingleton() {}
    public static LazySimpleSingleton getInstance() {
        if (lazy == null) {
            lazy = new LazySimpleSingleton();
        }
        retrun lazy;
    }
}
```

缺点：在多线程的环境下，有可能产生两个单例对象。

### 线程安全的懒汉式单例模式

我们给 getInstance() 方法加上 synchronized 关键字，使该方法变成线程同步方法。

```java
public class LazySimpleSingleton {
    private static LazySimpleSingleton lazy = null;
    private LazySimpleSingleton() {}
    public synchronized static LazySimpleSingleton getInstance() {
        if (lazy == null) {
            lazy = new LazySimpleSingleton();
        }
        retrun lazy;
    }
}
```

缺点：在线程数量比较多的情况下，如果CPU分配压力上升，则会导致大量线程阻塞。

### 双重检查锁的单例模式

```java
// 虽然是方法内部同步块，但多少还是会影响性能
public class LazyDoubleCheckSingleton {
    private volatile static LazyDoubleCheckSingleton lazy = null;
    private LazyDoubleSingleton() {}
    public static LazyDoubleSingleton getInstance() {
        if (lazy == null) {
            // 方法内部同步块
            // Thread1 Thread2 同时进入同步区，Thread2阻塞，Thread1继续执行
            synchronized(LazyDoubleSingleton.class) {
                // Thread2获取锁时，lazy已被初始化
                if (lazy == null) {
                    lazy = new LazyDoubleSingleton();
                }
            }
        }
        return lazy;
    }
}
```

### 静态内部类单例模式

```java
// 这种形式兼顾饿汉式单例模式的内存浪费问题和synchronized性能问题
public class LazyInnerClassSingleton {
    private LazyInnerClassSingleton() {}
    public static final LazyInnerClassSingleton getInstance() {
        return LazyHolder.LAZY;
    }
    
    private static class LazyHolder{
        private static final LayInnerClassSingleton LAZY = new LazyInnerClassSingleton();
    }
}
```

### 反射破坏单例

在上述静态内部类模式中，单例模式的构造方法除了加上private关键字，没有任何处理，此时如果使用反射机制来调用构造方法，再调用getInstance方法应该会出现两个不同的实例。

```java
public class LazyInnerClassSingletonTest {
    public static void main(String[] args) {
        try {
            Class<?> clazz = LazyInnerClassSingleton.class;
            // 通过反射获取私有的构造方法
            Constructor c = clazz.getDeclaredConstructor(null);
            // 强制访问
            c.setAccessible(true);
            Object o1 = c.newInstance();
            Object o2 = c.newInstance();
            System.out.println(o1 == o2); // print false
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

优化如下

```java
public class LazyInnerClassSingleton {
    private LazyInnerClassSingleton() {
        if (LazyHolder.LAZY != NULL) {
            throw new RuntimeException("不允许创建多个实例");
        }
    }
    public static final LazyInnerClassSingleton getInstance() {
        return LazyHolder.LAZY;
    }
    
    private static class LazyHolder{
        private static final LayInnerClassSingleton LAZY = new LazyInnerClassSingleton();
    }
}
```

### 注册式单例模式

注册式单例模式又称为登记式单例模式，就是将每一个实例都登记到某一个地方，使用唯一标识获取实例。

#### 枚举式单例模式

```java
public enum EnumSingleton {
    INSTANCE;
    private Object data;
    public Object getData() {
        return data;
    }
    public void setData(Object data) {
        this.data = data;
    }
    public static EnumSingleton getInstance() {
        return INSTANCE;
    }
}
```

#### 容器式单例模式

```java
public class ContainerSingleton {
    private ContainerSingleton() {}
    private static Map<String, Object> ioc = new ConcurrentHashMap<String, Object>();
    public static Object getBean(String className) {
        synchronized(ioc) {
            if (!ioc.containsKey(className)) {
                Object obj = null;
                try {
                    obj = Class.forName(className).newInstance();
                    ioc.put(className, obj);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return obj;
            } else {
                return ioc.get(className);
            }
        }
    }
}
```



