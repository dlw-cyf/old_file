##### 导入jar包

* 导入mybatis所有jar和spring基本包，spring-jdbc，spring-tx，spring-aop，spring整合mybati的包等

![spring整合mybatis的所有jar](G:\学习笔记\javaWeb\spring\image\spring整合mybatis的所有jar.png)

##### 编写spring配置文件applicationContext.xml

```xml
<beans xmlns="http://www.springframework.org/schema/beans"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.springframework.org/schema/beans
        http://www.springframework.org/schema/beans/spring-beans.xsd">
        
    <!-- 数据源封装类。数据源:获取数据库连接,spring-jdbc.jar中 -->
    <bean id="dataSource" class="org.springframework.jdbc.datasource.DriverManagerDataSource">
    	<property name="driverClassName" value="com.mysql.jdbc.Driver"></property>
    	<property name="url" value="jdbc:mysql://localhost:3306/ssm"></property>
    	<property name="username" value="root"></property>
    	<property name="password" value="123456"></property>
    </bean>
    
    <!-- 创建SqlSessionFactory对象 -->
    <bean id="factory" class="org.mybatis.spring.SqlSessionFactoryBean">
    	<!-- 数据连接信息来源于dataSource -->
    	<property name="dataSource" ref="dataSource"></property>
    </bean>
    
    <!-- 扫描器相当于mybatis.xml中mappers下packag标签 ,扫描com.mapper包后会给对应 接口创建对象 -->
    <bean class="org.mybatis.spring.mapper.MapperScannerConfigurer">
    	<!-- 要扫描哪个包 -->
    	<property name="basePackage" value="com.mapper"></property>
    	<!-- 和factory产生关系 -->
    	<property name="sqlSessionFactory" ref="factory"></property>
    </bean>
    
    <bean id="airportServiceImpl" class="com.service.impl.AirportServiceImpl">
    	<property name="airportMapper" ref="airportMapper"></property>
    </bean>
</beans>
```

##### 3. 编写代码

* 正常编写pojo
* 编写 mapper 包下时必须使用接口绑定方案或者注解方案
  * 必须有接口

* 正常编写Service接口和Service实现类
  * 需要在Service实现类中声明Mapper接口对象，并生成get/set方法

* spring无法管理Servlet
* 在web.xml中加载spring配置文件

```xml
<?xml version="1.0" encoding="UTF-8"?>
<web-app version="3.0" xmlns="http://java.sun.com/xml/ns/javaee"
	 xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" 
	 xsi:schemaLocation="http://java.sun.com/xml/ns/javaee                       
	 http://java.sun.com/xml/ns/javaee/web-app_3_0.xsd">
	 <!-- 上下文参数 -->
	 <context-param>
	 	<param-name>contextConfigLocation</param-name>
	 	<!-- spring配置文件 -->
	 	<param-value>classpath:applicationContext.xml</param-value>
	 </context-param>
	 <!-- 封装了一个监听器,帮助加载Spring的配置文件 -->
	 <listener>
	 	<listener-class>org.springframework.web.context.ContextLoaderListener</listener-class>
	 </listener>
</web-app>
```

