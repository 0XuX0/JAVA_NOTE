## Mybatis

### 什么是Mybatis

Mybatis是一个半ORM(对象关系映射)框架，可以使用XML或注解的方式将要执行的各种statement配置起来，并通过java对象和statement中sql的动态参数进行映射生成最终执行的sql语句，最后由mybatis框架执行sql并将结果映射为java对象并返回。

### #{} 和 ${}的区别

Mybatis在处理${}时，就是把${}直接替换成变量的值。而处理#{}时，会对sql进行预狐狸，将sql中的#{}替换为?号，调用PreparedStatement的set方法来赋值

### 通常一个mapper.xml文件都会对应一个Dao接口，这个Dao接口的工作原理是什么？

Mapper接口的工作原理是JDK动态代理，Mybatis运行时会使用JDK动态代理为Mapper接口生成代理对象proxy，代理对象会拦截接口方法，根据类的全限定名+方法名，唯一定位到一个MapperStatement并调用执行器执行所代表的sql

### Mybatis是否支持延迟加载？其原理是什么

延迟加载概念：先去查询简单的sql，再按需加载关联查询的其他信息；

举例：

两张表

图书表(book)

| bid  | bname    | cid  |
| ---- | -------- | ---- |
| 1    | 神雕侠侣 | 1    |
| 2    | 羊皮卷   | 2    |

图书类型表(category)

| cid  | cname |
| ---- | ----- |
| 1    | 武侠  |
| 2    | 励志  |

需求：显示图书类型名，点击类型名再显示该类型下的所有图书

POJO定义

```java
public class Category {
    private int cid;
    private String cname;
    private List<Book> books;
    // set & get
}
```

延迟加载的mapper.xml定义

```xml
<mapper namespace="cn.xh.dao.UserDao">
    <select id="findCategoryWithLazingload" resultMap="categoryMap">
        select * from category
    </select>
    <resultMap id="categoryMap" type="cn.xh.pojo.Category">
        <id column="cid" property="cid"></id>
        <result column="cname" property="cname"></result>

        <collection property="books" column="cid" select="findBookWithLazy"></collection>
    </resultMap>

    <select id="findBookWithLazy" parameterType="int" resultType="cn.xh.pojo.Book">
        select * from book where cid = #{cid}
    </select>
</mapper>
```

Mybatis仅支持association关联对象(一对一)和collection关联集合对象(一对多)的延迟加载。

配置文件

```xml
<settings>
    <setting name="lazyLoadingEnabled" value="true"/>
    <setting name="aggressiveLazyLoading" value="false"></setting>
</settings>
```

延迟加载的基本原理是，使用CGLIB创建目标对象的代理对象，当调用目标方法时，进入拦截器方法。比如调用a.getB().getName()，拦截器invoke方法发现a.getB()是null值，那么就会单独发送事先保存号的查询关联B对象的sql，把B查询上来，然后调用a.setB(b)，于是a的对象b属性就有值了，接着完成a.getB().getName()方法的调用。

### Mybatis的一级、二级缓存

一级缓存：基于PerpetualCache的HashMap本地缓存，其存储作用域为Session，当Session flush 或 close之后，该Session中的所有Cache就将清空

二级缓存：机制相同，其作用域为Mapper(Namespace)

### Mybatis动态sql有什么用？执行原理？有哪些动态sql

Mybatis动态sql可以再Xml映射文件内，以标签的形式编写动态sql，执行原理是根据表达式的值完成逻辑判断，并动态拼接sql 的功能

Mybatis提供了9种动态sql标签：trim | where | set | foreach | if | choose | when | otherwise | bind

### Mybatis 与 Hibernate 有哪些不同

1. Mybatis是一个半ORM框架，直接编写原生态sql，灵活度高，控制sql执行性能
2. Hibernate对象映射能力强，数据库无关性好，节省代码，提高效率