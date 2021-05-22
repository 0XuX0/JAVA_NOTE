## 委派模式(Delegate Pattern)

### 基本介绍

委派模式的基本作用是负责任务的调用和分配，和代理模式很像，代理模式注重过程，而委派模式注重结果

### 案例

```java
public interface IEmployee {
    public void doing(String command);
}
public class EmployeeA implements IEmployee {
    @Override
    public void doing(String command) {
        System.out.println("我是员工A，我现在开始干" + command + "工作");
    }
}
public class EmployeeB implements IEmployee {
    @Override
    public void doing(String command) {
        System.out.println("我是员工B，我现在开始干" + command + "工作");
    }
}
public class Leader implements IEmployee {
    private Map<String, IEmployee> targets = new HashMap<String, IEmployee>();
    public Leader() {
        targets.put("加密", new EmployeeA());
        targets.put("登录", new EmployeeB());
    }
    // 项目经理自己不干活
    public void doing(String command) {
        targets.get(command).doing(command);
    }
}
public class Boss {
    public void command(String command, Leader leader) {
        leader.doing(command);
    }
}
// 测试代码
public static void main(String[] args) {
    new Boss().command("登录", new Leader());
}
```

由上面这个例子，我们可以看到：

+ 代理模式注重的是过程，委派模式注重的是结果
+ 策略模式注重可扩展性，委派模式注重内部的灵活性和可复用性
+ 委派模式的核心是分发、调度、派遣，委派模式是静态代理和策略模式的一种特殊组合