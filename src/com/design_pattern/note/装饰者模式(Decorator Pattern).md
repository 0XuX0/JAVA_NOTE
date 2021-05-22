## 装饰者模式(Decorator Pattern)

### 基本介绍

装饰者模式是指在不改变原有对象的基础上，将功能附加到对象上，提供了比继承更有弹性的方案(扩展原有对象的功能)，属于结构型模式。

### 使用场景

1. 扩展一个类的功能或给一个类添加附加职责
2. 动态给一个对象添加功能，这些功能可以动态地撤销 

### 案例

```java
public abstract class Battercake {
    protected abstract String getMsg();
    protected abstract int getPrice();
}
public class BaseBattercake extends Battercake {
    protected String getMsg() {
        return "煎饼";
    }
    public int getPrice() { return 5; }
}
public abstract class BattercakeDecorator extends Battercake {
    private Battercake battercake;
    public BattercakeDecorator(Battercake battercake) {
        this.battercake = battercake;
    }
    protected abstract void doSomething();
    @Override
    protected String getMsg() {
        return this.battercake.getMsg();
    }
    @Override
    protected int getPrice() {
        return this.battercake.getPrice();
    }
} 
public class EggDecorator extends BattercakeDecorator {
    public EggDecorator(Battercake battercake) {
        super(battercake);
    }
    protected void doSomething() {}
    @Override
    protected String getMsg() {
        return super.getMsg() + "1 个鸡蛋";
    }
    @Override
    protected int getPrice() {
        return super.getPrice() + 1;
    }
}
public class SausageDecorator extends BattercakeDecorator {
    public SausageDecorator(Battercake battercake) {
        super(battercake);
    }
    protected void doSomething() {}
    @Override
    protected String getMsg() {
        return super.getMsg() + "1 根香肠";
    }
    @Override
    protected int getPrice() {
        return super.getPrice() + 2;
    }
}
public static void main(String[] args) {
    Battercake battercake;
    battercake = new BaseBattercake();
    battercake = new EggDecorator(battercake);
    battercake = new SausageDecorator(battercake);
    System.out.println(battercake.getMsg() + ",总价：" + battercake.getPrice());
}
```

### 源码中的应用

1. I/O包中 InputStream、OutputStream、BufferedReader 等
2. HttpHeadResponseDecorator类
3. MyBatis中的FifoCache、LruCache等

### 装饰者模式的优缺点

优点：

+ 装饰者是继承的有力补充，且比继承灵活，可以在不改变原有对象的情况下动态地给对象扩展功能，即插即用
+ 使用不同的装饰类及这些装饰类的排列组合，可以实现不同的功能
+ 符合开闭原则

缺点：

+ 出现更多的类，增加代码的复杂性
+ 动态装饰时，多层装饰会更复杂