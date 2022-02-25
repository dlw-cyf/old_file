#### 如何给Bean的属性赋值（注入）

1. 默认执行无参构造

   ```xml
   <bean id="peo" class="com.dlw.pojo.People"/>
   ```

2. 通过有参构造方法设置值

   ```xml
   <bean id="peo" class="com.bjsxt.pojo.People">
   <!-- ref 引用另一个 bean value 基本数据类型或 String 等 --> 
       <constructor-arg index="0" name="id" type="int" value="123">
       </constructor-arg> 		
       <constructor-arg index="1" name="name" type="java.lang.String" value="张三">
       </constructor-arg> 
   </bean>
   ```

3. 设置注入（通过set方法）

   * 如果属性是基本数据类型或String等

   ```xml
   <bean id="peo" class="com.dlw.pojo.People">
       <property name="id" value="222"></property>
       <property name="name" value="张三"></property>
   </bean>
   ```

   * 如果属性是set类型
     * 使用set标签注入

   ```xml
   <property name="sets"> 
       <set> 
           <value>1</value>
   		<value>2</value> 
           <value>3</value> 
           <value>4</value> 
       </set> 
   </property>
   ```

   * 如果属性是List
     * ​	使用list标签注入

   ```xml
   <property name="list"> 
       <list> 
           <value>1</value> 
           <value>2</value> 
           <value>3</value> 
       </list> 
   </property>
   ```

   * 如果list中就只有一个值

   ```xml
   <property name="list" value="1"> </property>
   ```

   * 如果属性是数组
     * 如果数组中只有一个值，也可以直接用一个value

   ```xml
   <property name="strs" > 
       <array> 
           <value>1</value> 
           <value>2</value>
   		<value>3</value> 
       </array> 
   </property>
   ```

   * 如果属性是map

   ```xml
   <property name="map"> 
       <map> 
           <entry key="a" value="b" > 
           </entry> <entry key="c" value="d" > 
           </entry> 
       </map> 
   </property
   ```

   * 如果属性是Properties类型

   ```xml
   <property name="demo"> 
       <props> 
           <prop key="key">value</prop> 
           <prop key="key1">value1</prop> 
       </props> 
   </property>
   ```

#### DI

* DI和IOC是一样的
* 当一个类（A）中需要依赖另一个类（B）对象时，把B赋值给A的过程就叫做依赖注入

#### 注解实现依赖注入

* 在xml中配置注解扫描

```xml
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xmlns:context="http://www.springframework.org/schema/context"
       xsi:schemaLocation="http://www.springframework.org/schema/beans
       http://www.springframework.org/schema/beans/spring-beans.xsd
       http://www.springframework.org/schema/context
       http://www.springframework.org/schema/context/spring-context.xsd">
    <context:component-scan base-package="com.dlw.service"></context:component-scan>
</beans>
```

* **用于创建对象的注解**
  1. @Component：把当前类对象存入spring容器中
     * value：用于指定bean的id，如果不写，默认为当前类名，并首字母小写
  2. @Controller：一般用在表现层
  3. @Service：一般用在业务层
  4. Repository：一般用在持久层
  5. **注意**：以上三个注解的作用和属性与Component是一模一样的

* **用于注入数据的注解**
  1. @AutoWired：自动按照类型注入，只要容器中有唯一的一个bean对象类型和要注入的变量类型匹配，即可注入。
     * 如果有多个相同类型的bean对象，则按照变量名称和id进行匹配
  2. @Qualifier：在按照类中注入的基础之上再按照名称注入。他在给类成员注入时不能单独使用（与AutoWired配合使用），但是在给方法参数注入时可以
     * value：用于指定注入bean的id。
  3. @Resource：直接按照bean对象的id注入，可以独立使用
     * name：用于指定bean的id。
  4. **注意**：以上三个注入都只能注入其他bean类型的数据，而基本数据类型无法使用上述注解实现。另外：集合类型的注入只能通过XML来实现。
  5. @Value：用于注入基本类型和String类型的数据
     * value属性：用于指定数据的值，它可以使用spring的SpEl（也就是spring的el表达式）
       * SpEl的写法：${表达式}

* **用于改变作用范围的注解**
  1. @Scope：用于指定bean的作用范围
     * value属性：指定范围的取值。常用取值：singleton（单例） prototype（多例）

* **和生命周期相关的注解（了解）**
  1. PreDestroy：用于指定销毁方法
  2. PostConstruct：用于指定初始化方法