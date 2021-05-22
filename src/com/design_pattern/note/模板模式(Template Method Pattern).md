## 模板模式(Template Method Pattern)

### 基本介绍

模板模式是指定义一个算法的框架，并允许子类为一个或者多个步骤提供实现。模板模式使得子类可以在不改变算法结构的情况下，重新定义算法的某些步骤，属于行为型设计模式。

### 使用场景

1. 一次性实现一个算法的不变部分，并将可变的行为留给子类来实现
2. 各子类中公共的行为被提取出来并集中到一个公共的父类中，从而避免代码重复

### 案例

```java
public abstract class NetworkCourse {
    protected final void createCourse() {
        this.postPreResource();
        this.createPPT();
        this.liveVideo();
        this.postNote();
        this.postSource();
        if (needHomeWork()) {
            checkHomework();
        }
    }
    abstract void checkHomeWork();
    protected boolean needHomework() { return false; } // 钩子函数
    final void postSource() { System.out.println("提交源代码"); }
    final void postNote() { System.out.println("提交课件和笔记"); }
    final void liveVideo() { System.out.println("直播授课"); }
    final void createPPT() { System.out.println("创建备课PPT"); }
    final void postPreResource() { System.out.println("发布预习资料"); }
}
```

### 模板模式的优缺点

优点：

+ 利用模板模式将相同处理逻辑的代码放到抽象父类中，可提高代码的复用性
+ 将不同代码放到不同子类中，通过对子类扩展增加新的行为，可以提高代码的扩展性
+ 把不变的行为写在父类中，去除子类的重复代码，提供很好的代码复用平台，符合开闭原则

缺点：

+ 每个抽象类都需要一个子类来实现，增加了类的数量
+ 增加了系统的复杂性
+ 由于继承关系自身的缺点，如果父类添加新的抽象方法，所有子类都要改一遍