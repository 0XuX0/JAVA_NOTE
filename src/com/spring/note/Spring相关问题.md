## Spring相关问题

### bean的作用域

+ singleton：唯一bean实例，Spring中的bean默认都是单例的
+ prototype：每次请求都会创建一个新的bean实例
+ request：每一次HTTP请求都会产生一个新的bean实例，该bean仅在当前http request内有效
+ session：每一次HTTP请求都会产生一个新的bean实例，该bean仅在当前http session内有效

### bean创建的三个阶段:

+ 实例化，可以理解为new一个对象
+ 属性注入，可以理解为调用setter方法完成属性注入
+ 初始化，可以按照Spring的规则配置一些初始化的方法

### 生命周期的概念

Bean的生命周期指的就是在上面三个步骤中后置处理器BeanPostProcessor穿插执行的过程

### 后置处理器分析

1. 直接实现了 **BeanPostProcessor** 接口，只能在初始化前后执行

   方法名：postProcessBeforeInitialization() postProcessAfterInitialization()

2. 直接实现了 **InstantiationAwareBeanPostProcessor** 接口，可以在实例化前后执行

   方法名：postProcessBeforeInstantiation() postProcessAfterInstantiation()

### bean的生命周期

+ Bean 容器找到配置文件中 Spring Bean 的定义
+ 若有和加载这个Bean的Spring容器相关的 **InstantiationAwareBeanPostProcessor** 对象，执行postProcessBeforeInstantiation 方法
+ Bean 容器利用 反射机制创建一个Bean的实例
+ 若有和加载这个Bean的Spring容器相关的 **InstantiationAwareBeanPostProcessor** 对象，执行postProcessAfterInstantiation 方法
+ set()方法设置一些属性
+ 执行aware接口，为初始化提供状态
+ 执行 BeanPostProcessor 对象的 postProcessBeforeInitialization 方法
+ bean的初始化，调用 afterPropertyesSet() 方法
+ 执行 BeanPostProcessor 对象的 postProcessAfterInitialization 方法

### @Component 和 @Bean的区别是什么

1. 作用对象不同：@Component 注解作用于类，@Bean 注解作用于方法

2. @Component 通常是通过类路径扫描来自动侦测以及自动装配到Spring容器中

   @Bean 注解通常是我们在标有该注解的方法中定义产生这个bean

3. @Bean注解比@Component注解的自定义性更强

### spring循环依赖问题

1. 构造器循环依赖(无法解决)

   ```java
   public class TestA {
       private TestB testB;
       TestA(TestB testB) { this.testB = testB; }
       public TestB getTestB() { return testB; }
       public void setTestB(TestB testB) { this.testB = testB; }
   }
   
   public class TestB {
       private TestA testA;
       TestB(TestA testA) { this.testA = testA; }
       public TestA getTestA() { return testA; }
       public void setTestA(TestA testA) { this.testA = testA; }
   }
   ```

2. setter循环依赖(可以解决)

   ```java
   public class TestA {
       private TestB testB;
       TestA() { }
       public TestB getTestB() { return testB; }
       public void setTestB(TestB testB) { this.testB = testB; }
   }
   
   public class TestB {
       private TestA testA;
       TestB() { }
       public TestA getTestA() { return testA; }
       public void setTestA(TestA testA) { this.testA = testA; }
   }
   ```

三级缓存的概念：

+ singletoneFactories：单例对象工厂的cache
+ earlySingletonObjects：提前曝光的单例对象的Cache
+ singletonObjects：单例对象的cache

A首先完成了初始化的第一步(实例化)，并且将自己提前曝光到SingletonFactories中，此时进行初始化的第二步(属性注入)，发现自己依赖对象B，就尝试去get(B)，发现B还没有被create，所以走create流程，B在初始化第二步的时候发现自己依赖对象A，就尝试get(A)，尝试一级缓存singletonObjects(肯定没有，A还没初始化完全)，尝试二级缓存earlySingletonObjects(也没有)，尝试三级缓存，由于A通过ObjectFactory将自己提前曝光了，所以B能够通过ObjectFactory.getObject拿到A对象(A没有完全初始化)，B拿到A对象后顺利完成了初始化阶段的123步骤，完全初始化之后，将自己放入到一级缓存singletonObjects中。此时返回A中，A此时能拿到B的对象顺利完成初始化阶段，进入一级缓存中。

构造器循环依赖无法解决的原因是：加入singletonFactories三级缓存的前提是执行了构造器。

### spring事务

Spring管理事务的方式有

+ 编程式事务，在代码中硬编码
+ 声明式事务，在配置文件中配置
  1. 基于XML的声明式事务
  2. 基于注解的声明式事务

Spring事务中的隔离级别有哪几种

+ TransactionDefinition.ISOLATION_DEFAULT：使用后端数据库默认的隔离级别
+ TransactionDefinition.ISOLATION_READ_UNCOMMITTED
+ TransactionDefinition.ISOLATION_READ_COMMITTED
+ TransactionDefinition.ISOLATION_REPEATABLE_READ
+ TransactionDefinition.ISOLATION_SERIALIZABLE

Spring事务的事务传播行为

支持当前事务的情况

+ TransactionDefinition.PROPAGATION_REQUIRED：如果当前存在事务，则加入该事务；如果当前没有事务，则创建一个新的事务
+ TransactionDefinition.PROPAGATION_SUPPORTS：如果当前存在事务，则加入该事务；如果当前没有事务，则以非事务的方式继续运行
+ TransactionDefinition.PROPAGATION_MANDATORY：如果当前存在事务，则加入该事务；如果当前没有事务，则抛出异常

不支持当前事务的情况

+ TransactionDefinition.PROPAGATION_REQUIRES_NEW：创建一个新的事务，如果当前存在事务，则把当前事务挂起
+ TransactionDefinition.PROPAGATION_NOT_SUPPORTED：以非事务方式运行，如果当前存在事务，则把当前事务挂起
+ TransactionDefinition.PROPAGATION_NEVER：以非事务方式运行，如果当前存在事务，则抛出异常