## 策略模式(Strategy Pattern)

### 基本介绍

策略模式是指定义了算法家族并分别封装起来，让它们之间可以互相替换

### 使用场景

1. 系统中有很多类，而它们的区别仅仅在于行为不同
2. 一个系统需要动态地在集中算法中选择一种

### 案例

```java
// Payment抽象类，定义支付规范和支付逻辑
public abstract class Payment {
    // 支付类型
    public abstract String getName();
    // 查询余额
    protected abstract double queryBalance(String uid);
    // 扣款支付
    public PayState pay(String uid, double amount) {
        if (queryBalance(uid) < amount){
            return new PayState(500, "支付失败", "余额不足");
        }
        return new PayState(200, "支付成功", "支付金额" + amount);
    }
}
// 分别创建具体的支付方式
// 支付宝支付
public class AliPay extends Payment {
    public String getName() {
        return "支付宝";
    }
    protected double queryBalance(String uid) {
        return 900;
    }
}
// 微信支付
public class WechatPay extends Payment {
    public String getName() {
        return "微信支付";
    }
    protected double queryBalance(String uid) {
        return 256;
    }
}
// 银联支付
public class UnionPay extends Payment {
    public String getName() {
        return "银联支付";
    }
    protected double queryBalance(String uid) {
        return 120;
    }
}
// 京东白条支付
public class JDPay extends Payment {
    public String getName() {
        return "京东白条";
    }
    protected double queryBalance(String uid) {
        return 500;
    }
}
// 支付状态包装类
public class PayState {
    private int code;
    private Object data;
    private String msg;
    
    public PayState(int code, String msg, Object data) {
        this.code = code;
        this.data = data;
        this.msg = msg;
    }
    public String toString() {
        return ("支付状态：[" + code + "]," + msg + ".交易详情：" + data);
    }
}
// 创建支付策略管理类
public class PayStrategy {
    public static final String ALI_PAY = "AliPay";
    public static final String JD_PAY = "JdPay";
    public static final String UNION_PAY = "UnionPay";
    public static final String WECHAT_PAY = "WechatPay";
    public static final String DEFAULT_PAY = "ALI_PAY";
    
    private static Map<String, Payment> payStrategy = new HashMap<String, Payment>();
    static {
        payStrategy.put(ALI_PAY, new AliPay());
        payStrategy.put(JD_PAY, new JDPay());
        payStrategy.put(UNION_PAY, new UnionPay());
        payStrategy.put(WECHAT_PAY, new WechatPay());
    }
    public static Payment get(String payKey) {
        if (!payStrategy.containsKey(payKey)) {
            return payStrategy.get(DEFAULT_PAY);
        }
        return payStrategy.get(payKey);
    }
}
// 创建订单类 Order
public class Order {
    private String uid;
    private String orderId;
    private double amount;
    
    public Order(String uid, String orderId, double amount) {
        this.uid = uid;
        this.orderId = orderId;
        this.amount = amount;
    }
    // 无需写switch
    // 无需写 if else
    public PayState pay() {
        return pay(PayStrategy.DEFAULT_PAY);
    }
    public PayState pay(String payKey) {
        Payment payment = PayStrategy.get(payKey);
        System.out.println("欢迎使用" + payment.getName());
        System.out.println("本次交易金额为：" + amount + "开始扣款...");
        return payment.pay(uid, amount);
    }
}
// 测试代码
public static void main(String[] args) {
    Order order = new Order("1", "2000", 32);
    System.out.println(order.pay(PayStrategy.ALI_PAY));
}
```

### 策略模式优缺点

优点：

+ 策略模式符合开闭原则
+ 策略模式可避免使用多重条件语句
+ 策略模式可提高算法的保密性和安全性

缺点：

+ 客户端必须知道所有的策略，并且自行决定使用哪一个策略类
+ 代码中会有很多的策略类，增加了代码的维护难度