#### 使用注解

* spring不会**自动去寻找注解**，必须告诉spring哪些包下的类可能有注解
  * 引入 xmlns:context 命名空间
  * 扫描多个包使用，分隔

```xml
<context:component-scan base-package="com.advice,com.test"></context:component-scan>
```

##### @Component 

* 创建对象，相当于配置 bean 标签

##### @Service

* 与 @Component 功能完全相同
* 建议写在**ServiceImpl类上**

##### @Repository 

* 与 @Component 功能完全相同
* 建议在在**数据访问层**

##### @Controller 

* 与 @Component 功能相同
* 建议在写**控制器类上**

##### @Resource

* java中的注解（不需要写对象的get/set）
* 默认按照 byName进行注入，如果没有这个名次的对象，按照byType注入
  * 建议把对象名称和spring容器中对象名相同

##### @AutoWired

* spring的注解（不需要写对象的get/set）
* 默认按照byType注入
* 如果有多个类型一样的bean对象，则通过变量名和id进行匹配

##### @value

* 获取 properties 文件中内容

##### @Pointcut

* 定义切点

##### @Aspect

* 定义切面类

##### @Before

* 前置通知

##### @After

* 后置通知

##### @AfterReturning

* 后置通知，必须切点正常执行

##### @AfterThrowing

* 异常通知

##### @Arround

* 环绕通知

##### @Configuration

* 指定当前类是一个配置类
* 细节：当配置类作为AnnotationConfigApplicationContext对象创建的参数时，该注解可以不写。

##### @Import

* 用于导入其他的配置类
  * value属性：用于指定其他配置类的字节码。
  * 当我们使用Import的注解之后，有Import注解的类就是父配置类，而导入的类是子配置类。

##### @ComponentScan

* 用于通过注释指定spring在创建容器时要扫描的包
  * value属性：它和basePackages的作用是一样的，都是用于指定创建容器时要扫描的包。

##### @Bean

* 把当前方法的返回值作为bean对象存入spring的ioc容器中
  * name属性：用于指定bean的id。默认值：当前方法的名称

##### @PropertySource

* 用于指定properties文件的位置
  * value属性：指定文件的名称和路径
    * classpath关键字：表示类路径下@propertySource("classpath:jdbcConfig.properties")

#### Spring整合Junit

1. 导入Spring整合Junit的jar（坐标）

   ```xml
   <dependency>  
       <groupId>org.springframework</groupId>  
       <artifactId>spring-test</artifactId>  
       <version>${spring.version}</version>  
   </dependency> 
   ```

2. 使用Junit提供的一个注解把原来的main方法替换了，替换成spring提供的

   * **@RunWith(SpringJUnit4ClassRunner.class)**

3. 告知spring的运行器，spring和ioc创建是基于xml还是注解的，并且说明位置

   * **@ContextConfiguration**
     * locations：指定xml文件的位置，加上classpath关键字，表示在类路径下。
     * classes：指定注解类所在的位置。

4. **注意**：当我们使用spring 5.x版本的时候，要求junit的jar必须是4.1.2及以上