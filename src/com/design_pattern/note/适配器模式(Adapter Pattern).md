## 适配器模式(Adapter Pattern)

### 基本介绍

适配器模式是指将一个类的接口转换成用户期望的另一个接口，使原本接口不兼容的类可以一起工作，属于结构型设计模式

### 使用场景

1. 已经存在的类的方法和需求不匹配(方法结果相同或相似)的情况

### 案例1

```java
// 通过增加电源适配器类 PowerAdapter 实现两者的兼容
// 表示220V交流电
public class AC220 {
    public int outputAC220V() {
        int output = 220;
        System.out.println("输出交流电" + output + "V");
        return output;
    }
}
// 表示5V直流电
public interface DC5 {
    int outputDC5V();
}
// 电源适配器
public class PowerAdapter implements DC5 {
    private AC220 ac220;
    public PowerAdapter(AC220 ac220) {
        this.ac220 = ac220;
    }
    public int outputDC5V() {
        int adapterInput = ac220.outputAC220V();
        // 变压器...
        int adapterOutput = adapterInput / 44;
        System.out.println("使用 PowerAdapter 输入 AC：" + adapterInput + "V" + "输出DC:" + adapterOutput + "V");
        return adapterOutput;
    }
}
// 测试代码
public static void main(String[] args) {
    DC5 dc5 = new PowerAdapter(new AC220());
    dc5.outputDC5V();
}
```

### 案例2

```java
// 不同登录方式
public interface LoginAdapter {
    boolean support(Object adapter);
    ResultMsg login(String id, Object adapter);
}
public class LoginForQQAdapter implements LoginAdapter {
    public boolean support(Object adapter) {
        return adapter instanceof LoginForQQAdapter;
    }
    public ResultMsg login(String id, Object adapter) {
        return null;
    }
}
public class LoginForSinaAdapter implements LoginAdapter {
    public boolean support(Object adapter) {
        return adapter instanceof LoginForSinaAdapter;
    }
    public ResultMsg login(String id, Object adapter) {
        return null;
    }
}

public interface IPassportForThird {
    ResultMsg loginForQQ(String id);
    ResultMsg loginForSina(String id);
}
public class PassportForThirdAdapter extends SiginService implements IPassportForThird {
    public ResultMsg loginForQQ(String id) {
        return processLogin(id, LoginForQQAdapter.class);
    }
    public ResultMsg loginForSina(String id) {
        return processLogin(id, LoginForSinaAdapter.class);
    }
    // 简单工厂模式 策略模式
    private ResultMsg processLogin(String key,Class<? extends LoginAdapter> clazz) {
        try {
            LoginAdapter adapter = clazz.newInstance();
            if(adapter.support(adapter)) {
                return adapter.login(key, adapter);
            } else {
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
```

### 适配器模式的优缺点

优点：

+ 提高类的透明性和复用性，现有的类会被复用但不需要修改
+ 目标类和适配器类解耦，可提高程序的扩展性
+ 符合开闭原则

缺点：

+ 增加系统的复杂性
+ 增加代码的阅读难度