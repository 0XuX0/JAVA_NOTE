## Spring基于事件的观察者模式

​	观察者模式也叫作发布－订阅模式，在Spring容器中通过ApplicationEvent类和ApplicationListener接口来处理事件．如果某个bean实现了ApplicationListener接口并被注入到容器中，那么每次ApplicationEvent事件被发布到容器中都会通知该bean．

### 事件

​	ApplicationEvent是继承于EventObject类的抽象类，JDK要求所有的事件都要继承这个类．通常我们在业务逻辑中继承ApplicationEvent后新增子属性已达到业务要求

```java
public abstract class ApplicationEvent extends EventObject {
    /** System time when the event happened. */
	private final long timestamp;
    // constructor ...
    // get method ...
}
```

### 发布者

​	我们通常使用ApplicationEventPublisher#publishEvent来发布事件，定位到具体实现AbstractApplicationContext#publishEvent方法，代码如下

```java
protected void publishEvent(Object event, @Nullable ResolvableType eventType) {
    // Decorate event as an ApplicationEvent if necessary
    // ...
    // Multicast right now if possible - or lazily once the multicaster is initialized
    if (this.earlyApplicationEvents != null) {
        this.earlyApplicationEvents.add(applicationEvent);
    }
    else {
        getApplicationEventMulticaster().multicastEvent(applicationEvent, eventType);
    }
    
    // Publish event via parent context as well...
    // ...
}
```

上述代码片段可见，事件发布可以放到集合中存放起来，也可直接委托ApplicationEventMulticaster#multicastEvent来执行．在refresh方法中会初始化earlyApplicationEvents的值，并决定ApplicationEventMulticaster的实现

```java
public void refresh() throws BeansException, IllegalStateException {
    synchronized (this.startupShutdownMonitor) {
        // Prepare this context for refreshing.
        // 这一步将earlyApplicationEvents初始化为LinkedHashSet
	    prepareRefresh();
        // ...
        // Initialize event multicaster for this context.
		initApplicationEventMulticaster();
        // Check for listener beans and register them.
        // 这一步将earlyApplicationEvents里存放的事件委托给已初始化的event multicaster进行处理
        // 然后将earlyApplicationEvents置为null
		registerListeners();
    }
}
```

我们先来看下ApplicationEventMulticaster的初始化过程

```java
// 到本地容器中找一个名为applicationEventMulticaster的实现
//　如果没有就new一个SimpleApplicationEventMulticaster
protected void initApplicationEventMulticaster() {
    ConfigurableListableBeanFactory beanFactory = getBeanFactory();
    if (beanFactory.containsLocalBean(APPLICATION_EVENT_MULTICASTER_BEAN_NAME)) {
        this.applicationEventMulticaster = 
           beanFactory.getBean(APPLICATION_EVENT_MULTICASTER_BEAN_NAME, ApplicationEventMulticaster.class);
        // log...
    }
    else {
        this.applicationEventMulticaster = new SimpleApplicationEventMulticaster(beanFactory);
        beanFactory.registerSingleton(APPLICATION_EVENT_MULTICASTER_BEAN_NAME, this.applicationEventMulticaster);
        // log...
    }
}
```

我们接下来看下默认的SimpleApplicationEventMulticaster的multicastEvent方法

```java
public void multicastEvent(final ApplicationEvent event, @Nullable ResolvableType eventType) {
    ResolvableType type = (eventType != null ? eventType : resolveDefaultEventType(event));
    Executor executor = getTaskExecutor();
    for (ApplicationListener<?> listener : getApplicationListeners(event, type)) {
        if (executor != null) {
            executor.execute(() -> invokeListener(listener, event));
        }
        else {
            invokeListener(listener, event);
        }
    }
}

protected void invokeListener(ApplicationListener<?> listener, ApplicationEvent event) {
    // ...
    doInvokeListener(listener, event);
}
private void doInvokeListener(ApplicationListener listener, ApplicationEvent event) {
    // ...
    listener.onApplicationEvent(event);
}
```

我们可以看到如果executor不为null就支持异步发布，否则默认是同步发布（由于refresh方法中是无参构造），最后调用onApplicationEvent方法来通知listener

### 异步响应

1. 自定义SimpleApplicationEventMulticaster

   ```java
   @Configuration
   public class Config {
       @Bean
       public SimpleApplicationEventMulticaster applicationEventMulticaster = 
           new SimpleApplicationEventMulticaster();
       simpleApplicationEventMulticaster.setTaskExecutor(simpleAsyncTaskExecutor());
       return simpleApplicationEventMulticaster;
   }
   ```

   不足：所有的事件响应都异步，无法做到按需异步

2. 使用@Async注解

### 监听者

​	在实际开发过程中，我们多用 @EventListener 注解作用于方法或类上，并定义监听的事件类，接下来，我们跟进源码来分析spring容器是如何实现监听机制的

```java
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface EventListener {
    @AliasFor("classes")
    Class<?>[] value() default {};
    @AliasFor("value")
    Class<?>[] classes() default {};
    String condition() default "";
}
```

​	从注解上可以发现 @EventListener 注解是通过 EventListenerMethodProcessor 方法自动将实例注入到容器中。

```java
public class EventListenerMethodProcessor implements SmartInitializingSingleton, ApplicationContextAware, BeanFactoryPostProcessor {
    protected final Log logger = LogFactory.getLog(getClass());

	@Nullable
	private ConfigurableApplicationContext applicationContext;

	@Nullable
	private ConfigurableListableBeanFactory beanFactory;

	@Nullable
	private List<EventListenerFactory> eventListenerFactories;

	private final EventExpressionEvaluator evaluator = new EventExpressionEvaluator();

	private final Set<Class<?>> nonAnnotatedClasses = Collections.newSetFromMap(new ConcurrentHashMap<>(64));
    
    // any other implements ...
}
```

​	该类实现了 ApplicationContextAware 接口：

```java
// 将上下文 applicationContext 注入进来
@Override
public void setApplicationContext(ApplicationContext applicationContext)
    Assert.isTrue(applicationContext instanceof ConfigurableApplicationContext,
                  "ApplicationContext does not implement ConfigurableApplicationContext");
	this.applicationContext = (ConfigurableApplicationContext) applicationContext;
}
```

​	该类实现了 BeanFactoryPostProcessor 接口，BeanFactoryPostProcessor是beanFactory的后置处理器，在BeanFactory标准初始化之后调用，这时所有的bean定义已经加载到beanFactory，但是bean的实例还未创建。实现 postProcessBeanFactory方法能够定制和修改BeanFactory的内容。

```java
// 获取容器中所有EventListenerFactory或者子类的bean
@Override
public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
    this.beanFactory = beanFactory;
    
    Map<String, EventListenerFactory> beans = beanFactory.getBeansOfType(EventListenerFactory.class, false, false);
    List<EventListenerFactory> factories = new ArrayList<>(beans.values());
	AnnotationAwareOrderComparator.sort(factories);
	this.eventListenerFactories = factories;
}
```

​	该类实现了 SmartInitializingSingleton 接口，其作用是在Spring容器管理的所有单例对象（非懒加载对象）初始化完成之后调用的回调接口

```java
@Override
public void afterSingletonsInstantiated() {
    ConfigurableListableBeanFactory beanFactory = this.beanFactory;
    Assert.state(this.beanFactory != null, "No ConfigurableListableBeanFactory set");
    // 获取容器中所有的bean并循环处理
    String[] beanNames = beanFactory.getBeanNamesForType(Object.class);
    for (String beanName : beanNames) {
        if (!ScopedProxyUtils.isScopedTarget(beanName)) {
            Class<?> type = null;
            try {
                type = AutoProxyUtils.determineTargetClass(beanFactory, beanName);
            }
            catch (Throwable ex) {
                // An unresolvable bean type, probably from a lazy bean - let's ignore it.
            }
            if (type != null) {
                if (ScopedObject.class.isAssignableFrom(type)) {
                    try {
                        Class<?> targetClass = AutoProxyUtils.determineTargetClass(beanFactory, ScopedProxyUtils.getTargetBeanName(beanName));
                        if (targetClass != null) {
                            type = targetClass;
                        }
                    }
                    catch (Throwable ex) {
                        // An invalid scoped proxy arrangement - let's ignore it.
                    }
                }
                try {
                    processBean(beanName, type);
                }
                catch (Throwable ex) {
                    throw new BeanInitializationException("Failed to process @EventListener " + "annotation on bean with name '" + beanName + "'", ex);
                }
            }
        }
    }
}
```

接下来我们看 processBean 方法的源码

```java
private void processBean(final String beanName, final Class<?> targetType) {
    // 在nonAnnotatedClasses中没出现过。并且类上或者方法上是否有EventListener注解
    if (!this.nonAnnotatedClasses.contains(targetType) &&
       AnnotationUtils.isCandidateClass(targetType, EventListener.class) &&
       !isSpringContainerClass(targetType)) {
        
        Map<Method, EventListener> annotatedMethods = null;
        try {
            annotatedMethods = MethodIntrospector.selectMethods(targetType, (MethodIntrospector.MetadataLookup<EventListener>) method -> AnnotatedElementUtils.findMergedAnnotation(method, EventListener.class));
        }
        catch (Throwable ex) {
            // An unresolvable type in a method signature, probably from a lazy bean - let's ignore it.
        }
        if (CollectionUtils.isEmpty(annotatedMethods)) {
			this.nonAnnotatedClasses.add(targetType);
			if (logger.isTraceEnabled()) {
				logger.trace("No @EventListener annotations found on bean class: " + targetType.getName());
			}
		}
        else {
            ConfigurableApplicationContext context = this.applicationContext;
			Assert.state(context != null, "No ApplicationContext set");
			List<EventListenerFactory> factories = this.eventListenerFactories;
			Assert.state(factories != null, "EventListenerFactory List not initialized");
			for (Method method : annotatedMethods.keySet()) {
				for (EventListenerFactory factory : factories) {
					if (factory.supportsMethod(method)) {
						Method methodToUse = AopUtils.selectInvocableMethod(method, context.getType(beanName));
                        // 根据beanName和targetType生成applicationListener
						ApplicationListener<?> applicationListener =
								factory.createApplicationListener(beanName, targetType, methodToUse);
						if (applicationListener instanceof ApplicationListenerMethodAdapter) {
							((ApplicationListenerMethodAdapter) applicationListener).init(context, this.evaluator);
						}
                        // 注入容器
						context.addApplicationListener(applicationListener);
						break;
					}
				}
			}
            if (logger.isDebugEnabled()) {
				logger.debug(annotatedMethods.size() + " @EventListener methods processed on bean '" + beanName + "': " + annotatedMethods);
			}
        }
    }
}
```

### 总结

​	添加了@EventListener注解的自定义名称的方法，会在EventListenerMethodProcessor中的afterSingletonsInstantiated()方法中遍历所有 ApplicationContext容器的单利bean。将所有添加了@EventListener的方法注入到ApplicationContext的applicationListeners和初始化的SimpleApplicationEventMulticaster的defaultRetriever.applicationListeners中，在发送事件时候获取监听列表时用。