## 原型模式（Prototype Pattern）

### 基本介绍

+ 原型模式（Prototype 模式）是指：用原型实例指定创建对象的种类，并且通过拷贝这些原型，创建新的对象
+ 原型模式是一种创建型设计模式，使你能够复制已有对象，而无需使代码依赖它们所属的类
+ 工作原理：通过将一个原型对象传给那个要发动创建的对象，这个要发动创建的对象通过请求原型对象拷贝它们自己来实施创建，即**.clone**()

### 类图

+ Prototype：原型 接口将对克隆方法进行声明，该类需要实现Cloneable接口，在运行时通知虚拟机可以安全地在实现了此接口的类上使用 clone 方法；该类需要重写Object类中的clone方法，作用是返回对象的一个拷贝
+ ConcretePrototype：具体原型 类将实现克隆方法。
+ Client：使用原型的客户端，首先要获取到原型实例对象，然后通过原型实例克隆自己。

### 案例

```java
Class Sheep implements Cloneable {
    private String name;
    private Integer age;
    private String color;
    
    @Override
    protected Sheep clone() {
        Sheep sheep = null;
        try {
            sheep = (Sheep) super.clone();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return sheep;
    }
}

public class Goat extends Sheep {
    public void graze() {
        System.out.println("山羊去吃草");
    }
}
public class Lamb extends Sheep {
    public void graze() {
        System.out.println("羔羊去吃草");
    }
}

public class Client {
    static List<Sheep> sheepList = new ArrayList<>();
    public static void main(String[] args) {
        Goat goat = new Goat();
        for(int i = 0; i < 5; i++) {
            sheepList.add(goat.clone());
        }
        
        // 羊羔同理进行克隆
    }
}
```

### 优势

+ 使用原型模式创建对象比直接new一个对象在性能上要好得多，因为Object类的clone方法是一个本地方法，它直接操作内存中的二进制流，特别是复制大对象时，性能的差别明显。
+ 简化对象的创建

### 注意事项 

+ 使用原型模式复制对象时不会调用类的构造方法。因为对象的复制是通过调用Object类的clone方法来完成的，它直接在内存中复制数据，因此不会调用到类的构造方法。不但构造方法中的代码不会执行，甚至连访问权限都对原型模式无效。在单例模式中，只要将构造方法设置为私有就可实现单例。但是clone方法直接无视构造方法的权限，所以单例模式与原型模式是冲突的
+ 深拷贝与浅拷贝。Object类的clone方法只会拷贝对象中的基本的数据类型，对于数组、容器对象、引用对象等都不会拷贝，这就是浅拷贝。如果要实现深拷贝，必须将原型模式中的数组、容器对象、引用对象等另行拷贝



