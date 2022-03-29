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
+ 设置对象属性(依赖注入)
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

### Spring IOC的理解

1. IOC就是控制翻转，指创建对象的控制权转移给Spring框架进行管理，并由Spring根据配置文件去创建实例和管理各个实例之间的依赖关系。依赖注入，和控制反转是同一个概念的不同角度的描述，即 应用程序在运行时依赖ioc容器来动态注入对象需要的外部依赖
2. 直观表达：以前创建对象的主动权和时机都是由程序员把控，IOC让对象的创建不用去new了，可以由spring自动生产，使用java的反射机制，根据配置文件在运行时动态地区创建对象及管理对象
3. Spring IOC 三种注入方式：构造器注入、setter注入、注解注入

### Spring AOP的理解

AOP，面向切面编程，用于将那些与业务无关，但却对多个对象产生影响的公共行为和逻辑，抽取并封装为一个可重用的模块。Spring AOP使用的是动态代理，即AOP框架不会去修改字节码，而是每次运行时在内存中临时为方法生成一个AOP对象，这个AOP对象包含了目标对象的全部方法，并且在特定的切点做了增强处理，并回调原对象的方法。

+ JDK动态代理

  JDK动态代理只提供接口的代理，不支持类的代理，要求被代理类实现接口。JDK动态代理的核心是InvocationHandler和Proxy类，在获取代理对象时，使用Proxy类来动态创建目标类的代理类，当代理对象调用真实对象的方法时，InvocationHandler通过invoke方法反射来调用目标类中的代码，动态地将横切逻辑和业务编织在一起

+ CGLIB动态代理

  CGLIB(code generation library)是一个代码生成的类库，可在运行时动态地生成指定类的一个子类对象，并覆盖其中特定方法并添加增强代码从而实现AOP。

### Spirng AOP里几个名词的概念

1. 连接点(Join Point)：指程序运行过程中所执行的方法
2. 切面(Aspect)：被抽取出来的公共模块，一个切面由多个切点和通知组成
3. 切点(PointCut)：切点用于定义要对哪些Joint point 进行拦截
4. 通知(Advice)：指要在连接点上执行的动作，即增强的逻辑
5. 目标对象(Target)：包含连接点的对象，也称作被通知的对象
6. 织入(Weaving)：通过动态代理，在目标对象的方法中执行增强逻辑的过程
7. 引入(Introduction)：添加额外的方法或者字段到被通知的类。Spring允许引入新的接口到任何被代理的对象

### Spring通知有哪些类型

1. 前置通知(Before Advice)：在连接点之前执行的通知
2. 后置通知(AfterAdvice)：当连接点退出的时候执行的通知
3. 环绕通知(Around Advice)：包围一个连接点的通知
4. 返回后通知(AfterReturning Advice)：在连接点正常完成后执行的通知
5. 抛出异常后通知(AfterThrowing Advice)：在方法抛出异常退出时执行的通知

> 同一个Aspect，不同advice的执行顺序：
>
> 1. 没有异常情况下的执行顺序：
>    + around before advice
>    + before advice
>    + target method
>    + around after advice
>    + after advice
>    + afterReturning
> 2. 有异常情况下的执行顺序
>    + around before advice
>    + before advice
>    + target method
>    + around after advice
>    + after advice
>    + afterThrowing
>    + java.lang.RuntimeException 抛出异常

### Spring 容器的启动流程

1. 初始化Spring 容器，注册内置的BeanPostProcessor和BeanDefinition到容器中
   + 实例化BeanFactory(DefaultListableBeanFactory)工厂，用于生成Bean对象
   + 实例化BeanDefinitionReader注解配置读取器，用于对特定注解(如@Service @Repository)的类进行读取转化成BeanDefinition对象(存储bean对象的所有特征信息，如是否单例，是否懒加载等)
   + 实例化ClassPathBeanDefinitionScanner路径扫描器，用于对指定的包目录进行扫描查找bean对象
2. 将配置类的BeanDefinition注册到容器中
3. 调用refresh()方法刷新容器

### BeanFactory和ApplicationContext有什么区别

​	BeanFactory和Application是Spring的两大核心接口，都可以当作Spring的容器

1. BeanFactory是Spring里面最底层的接口，是IOC的核心，定义了IOC的基本功能，包含了各种Bean的定义、加载、实例化、依赖注入和生命周期管理。ApplicationContext接口作为BeanFactory的子类，提供了更完整的框架功能

2. BeanFactory采用的是延迟加载形式注入Bean的，只有在使用到某个Bean时，才对该Bean进行加载实例化。

   ApplicationContext，是在容器启动时一次性创建了所有的Bean。故在容器启动时就可以发现Spring中存在的配置错误，比如Bean的某一个依赖没有注入

   ApplicationContext启动后预载入所有的单实例Bean，所以在运行的时候速度比较快，但占用内存空间

3. BeanFactory和ApplicationContext都支持BeanPostProcessor、BeanFactoryPostProcessor的使用，但两者间的区别是：BeanFactory需要手动注册，而ApplicationContext则是自动注册

### Spring的自动装配

1. 在Spring框架xml配置中共有5种自动装配：

   + no：默认的方式是不进行自动装配的，通过手工设置ref属性来进行状态bean
   + byName：通过bean的名称进行自动装配
   + byType：通过参数的数据类型进行自动装配
   + constructor：利用构造函数进行装配，且构造函数的参数通过byType进行装配
   + autodetect：自动探测，如果有构造方法，通过constructor方式自动装配，否则使用byType方式自动装配

2. 基于注解的自动装配方式：

   在启动Sring IOC容器时，容器自动装载了一个AutowiredAnnotationBeanPostprocessor后置处理器，当容器扫描到@Autowired、@Resource时就会在IOC容器自动查找需要的bean，并装配给该对象的属性。

   在使用@Autowired时，首先在容器中查询对应类型的Bean：

   如果查询结果刚好为一个，就将bean装配给@Autowired指定的数据

   如果查询的结果不止一个，那么@Autowired会根据名称来查找

   如果上述查找结果为空，那么会抛出异常。解决方法是 required = false

   @Autowired 和 @Resource 的区别

   1. @Autowired默认是按照类型装配注入的，默认情况下它要求依赖对象必须存在
   2. @Resource默认是按照名称来装配注入的，只有当找不到与名称匹配的bean才会按照类型来装配注入

### Spring框架中都用到了哪些设计模式

1. 工厂模式：通过BeanFactory和ApplicationContext来创建对象
2. 单例模式：Bean默认为单例模式
3. 策略模式：例如Resource的实现类，针对不同的资源文件实现不同方式的资源获取策略
4. 代理模式：Spring AOP用到了动态代理
5. 模板方法：将相同部分的代码放在父类中，而将不同的代码放入不同的子类中，以解决代码重复的问题
6. 适配器模式：Spring AOP的增强或通知(Advice)
7. 观察者模式：Spring 事件驱动模型

### SpringBoot 自动装配原理

Spring Boot 通过 @EnableAutoConfiguration 开启自动配置，通过SpringFactoryLoader最终加载 META-INF/spring.factories中的自动配置类实现自动装配，自动配置类其实就是通过@Conditional按需加载的配置类

