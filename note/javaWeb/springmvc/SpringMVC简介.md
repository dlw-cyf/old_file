#### SpringMVC简介

##### SpringMVC中重要组件

1. DispatcherServlet：前端控制器
   * 接收所有请求（如果配置成 / 不包含jsp）

2. HandlerMapping
   * 解析请求格式的，判断希望要执行那个具体的方法
3. HandlerAdapter
   * 负责调用具体的方法
4. ViewResovler：视图解析器
   * 解析结果，准备跳转到具体的物理视图

#### SpringMVC环境搭建

1. 导入jar包

2. 在web.xml中配置前端控制器DispatchServlet

   * 如果不配置  init-param  会在/WEB-INF/<servlet-name>-servlet.xml

   ```xml
   <web-app version="3.0" xmlns="http://java.sun.com/xml/ns/javaee"
   	 xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" 
   	 xsi:schemaLocation="http://java.sun.com/xml/ns/javaee                       
   	 http://java.sun.com/xml/ns/javaee/web-app_3_0.xsd">
   	 <!-- 配置前端控制器 -->
   	 <servlet>
   	 	<servlet-name>abc</servlet-name>
   	 	<servlet-class>org.springframework.web.servlet.DispatcherServlet</servlet-class>
   	 	
   	 	<!-- 设置默认配置文件路径 -->
   	 	<init-param>
   	 		<param-name>contextConfigLocation</param-name>
   	 		<param-value>classpath:springmvc.xml</param-value>
   	 	</init-param>
   	 	<load-on-startup>1</load-on-startup>
   	 </servlet>
   	 
   	 <servlet-mapping>
   	 	<servlet-name>abc</servlet-name>
   	 	<url-pattern>/</url-pattern>
   	 </servlet-mapping>
   	 
   	 <!-- 字符编码过滤器
   	 	只对post请求生效
   	  -->
   	 <filter>
   	 	<filter-name>encoding</filter-name>
   	 	<filter-class>org.springframework.web.filter.CharacterEncodingFilter</filter-class>
   	 	<init-param>
   	 		<param-name>encoding</param-name>
   	 		<param-value>utf-8</param-value>
   	 	</init-param>
   	 </filter>
   	 <filter-mapping>
   	 	<filter-name>encoding</filter-name>
   	 	<url-pattern>/*</url-pattern>
   	 </filter-mapping>
   </web-app>
   ```

3. 在src下新建springmvc.xml

   * 引入xmlns:mvc命名空间

   ```xml
   <beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:mvc="http://www.springframework.org/schema/mvc"
       xmlns:context="http://www.springframework.org/schema/context"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xsi:schemaLocation="
           http://www.springframework.org/schema/beans
           http://www.springframework.org/schema/beans/spring-beans.xsd
           http://www.springframework.org/schema/mvc
           http://www.springframework.org/schema/mvc/spring-mvc.xsd
           http://www.springframework.org/schema/context
           http://www.springframework.org/schema/context/spring-context.xsd">
       <!-- 扫描注解 -->
   	<context:component-scan base-package="com.controller"></context:component-scan>
   
   	<!-- 注解驱动 -->
   	<!-- 相当于配置了handlerMapping和handlerAdapter(基于注解) -->
   	<mvc:annotation-driven></mvc:annotation-driven>
   	
   	<!-- 静态资源
   		mapping:表示请求中的路径
   		location:表示实际路径
   	 -->
   	<mvc:resources location="/js/" mapping="/js/**"></mvc:resources>
   	<mvc:resources location="/css/" mapping="/css/**"></mvc:resources>
   	<mvc:resources location="/images/" mapping="/images/**"></mvc:resources>
   	
   	<!-- 配置视图解析器 
   		如果希望不执行自定义视图解析器,在方法返回值前面添加forward:或redirect
   	-->
   	<bean id="viewResolver" class="org.springframework.web.servlet.view.InternalResourceViewResolver">
   		<property name="prefix" value="/"></property>
   		<property name="suffix" value=".jsp"></property>
   	</bean>
   </beans>
   ```

4. 编写控制类

   ```java
   @Controller
   public class DemoController {
   	@RequestMapping("test")
   	public String test(){
   		return "cs.jsp";
   	}
   	
   	/**
   	 * 当参数为类类型时,只需要类属性和key值相同自动注入(走get/set方法)
   	 * @param peo
   	 * @return
   	 */
   	@RequestMapping("demo")
   	public String demo(People peo){
   		System.out.println("执行demo"+peo);
   		return "/main.jsp";
   	}
   }
   ```

   

#### 跳转方式

1. 默认跳转方式为请求转发
2. 设置返回值字符串内容
   * 添加 redirect:资源路径	重定向
   * 添加 forward:资源路径    请求转发

#### 视图解析器

* 自定义视图解析器
  * 如果希望不执行自定义视图解析器,在方法返回值前面添加forward:或redirect

```xml
<!-- 配置视图解析器 
		如果希望不执行自定义视图解析器,在方法返回值前面添加forward:或redirect
	-->
	<bean id="viewResolver" class="org.springframework.web.servlet.view.InternalResourceViewResolver">
		<property name="prefix" value="/"></property>
		<property name="suffix" value=".jsp"></property>
	</bean>
```

