#### 介绍

* AOP：面向切面编程
* Aspect oriented Programming
* 正常程序执行流程都是纵向执行流程

> 在整个程序执行的任意一个方法的前后额外进行扩充，添加一些功能。
>
> * 在原有纵向执行流程中添加横切面
> * 不需要修改原有程序代码
>   * 高扩展性
>   * 原有功能相当于释放了部分逻辑，让职责更加明确。

#### 常用概念

1. **切点**：pointcut
   * 原有功能
2. **前置通知**：before advice
   * 在切点之前执行的功能
3. **后置通知**：after advice
   * 在切点之后执行的功能。
4. **异常通知**：throws advice
   * 切点执行过程中出现异常，会触发异常通知。
5. **切面**：
   * 所有功能的总称
6. **织入**：
   * 把切面嵌入到原有功能的过程交过织入 

#### 通知

##### 前置通知

* 在程序执行的任意一个方法的前面添加一些功能

##### 后置通知

* 在程序执行的任意一个方法的后面添加一些功能

#### Spring的2中AOP实现方式

##### Schema-based

* 每个通知都需要实现接口或类

* 配置spring配置文件时在 aop:config 标签配置

  ```xml
  <bean id="demo" class="com.test.Demo"></bean>
  
  <bean id="mybefore" class="com.dlw.advice.MyBeforeAdvice"></bean>
  <bean id="myafter" class="com.dlw.advice.MyAfterAdvice"></bean>
  
  <aop:config>
      <aop:pointcut expression="execution(* com.test.Demo.demo1())" id="mypoint"/>
      <aop:advisor advice-ref="mybefore" pointcut-ref="mypoint"/>
      <aop:advisor advice-ref="myafter" pointcut-ref="mypoint"/>
  </aop:config>
  ```

###### 实现步骤

1. 导入jar包

2. 新建通知类

   * 前置通知

     * arg0：切点方法对象Method对象
     * arg1：切点方法参数
     * arg2：切点在那个对象中

     ```java
     public class MyBeforeAdvice implements MethodBeforeAdvice{
     
     	@Override
     	public void before(Method arg0, Object[] arg1, Object arg2) throws Throwable {
     		System.out.println("前置通知");
     	}
     	
     }
     ```

   * 后置通知

     * arg0：切点方法的返回值
     * arg1：切点方法对象
     * arg2：切点方法参数
     * arf3：切点方法所在类的对象

     ```java
     public class MyAfterAdvice implements AfterReturningAdvice{
     
     	@Override
     	public void afterReturning(Object arg0, Method arg1, Object[] arg2, Object arg3) throws Throwable {
     		System.out.println("执行后置通知");
     	}
     
     }
     ```

   * 环绕通知

     ```xml
     <bean id="myarround" class="com.advice.MyArround"></bean>    
     <aop:config>
         <aop:pointcut expression="execution(* com.test.Demo.demo2())" id="mypoint2"/>
         <aop:advisor advice-ref="myarround" pointcut-ref="mypoint2"/>
     </aop:config>
     ```

     ```java
     public class MyArround implements MethodInterceptor{
     
     	@Override
     	public Object invoke(MethodInvocation arg0) throws Throwable {
     		System.out.println("环绕-前置");
     		Object result = arg0.proceed();//放行,调用切点方式
     		System.out.println("环绕-后置");
     		return result;
     	}
     	
     }
     ```

3. 配置spring配置文件

   * 引入AOP的命名空间
   * 配置通知类的bean
   * 配置切面
   * '*'  通配符，匹配任意方法名，任意类名，任意一级包名
   * 如果希望匹配方法任意类型参数   -->（..）

   ```xml
   <!-- 配置Demo类，测试的时候使用 -->
   <bean id="demo" class="com.test.Demo"></bean>
   
   <!-- 配置通知类对象 -->
   <bean id="mybefore" class="com.dlw.advice.MyBeforeAdvice"></bean>
   <bean id="myafter" class="com.dlw.advice.MyAfterAdvice"></bean>
   
   <aop:config>
       <!-- 配置切点 -->
       <aop:pointcut expression="execution(* com.test.Demo.demo1())" id="mypoint"/>
       <!-- 通知 -->
       <aop:advisor advice-ref="mybefore" pointcut-ref="mypoint"/>
       <aop:advisor advice-ref="myafter" pointcut-ref="mypoint"/>
   </aop:config>
   ```

##### AspectJ

* 每个通知不需要实现接口或类

* 配置spring配置文件实在 aop:config 标签的子标签 aop:aspect 中配置

  * 异常通知

    * aop:aspect 的 ref 属性表示：方法在哪个类中
    * aop:xxx 表示什么通知
    * method：当触发这个通知时，调用哪个个方法
    * throwing：异常对象名，必须和通知中方法参数名相同

    ```xml
    <!-- 异常通知 -->
    <bean id="mythrow" class="com.advice.MyThrowAdvice"></bean>
    
    <aop:config>
        <!-- 引入自定义异常类 -->
        <aop:aspect ref="mythrow">
            <aop:pointcut expression="execution(* com.test.Demo.demo1())" id="mypoint"/>
            <aop:after-throwing method="myexception" pointcut-ref="mypoint" throwing="e"/>
        </aop:aspect>
    </aop:config>
    ```

    ```java
    public class MyThrowAdvice {
    	public void myexception(Exception e){
    		System.out.println("执行异常通知,异常消息"+e.getMessage());
    	}
    }
    ```

  * 前置，后置，环绕，异常

    * aop:after-returning 后置通知，出现异常不执行
    * aop:after 后置通知，是否出现异常都执行
    * aop:after-returning    aop:after   aop:after-throwing 执行顺序和配置顺序有关
    * execution() 括号不能括上args
    * 中间使用and不能使用&& 由spring把 and 解析成 &&
    * arg(名称自定义)   顺序和demo1(参数，参数)对应
    * aop:before arg-names="名称" 名称来源于expression="" 中args(),名称必须一样
    * 

    ```xml
    <beans xmlns="http://www.springframework.org/schema/beans"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xmlns:aop="http://www.springframework.org/schema/aop"
        xsi:schemaLocation="http://www.springframework.org/schema/beans
            http://www.springframework.org/schema/beans/spring-beans.xsd
            http://www.springframework.org/schema/aop
            http://www.springframework.org/schema/aop/spring-aop.xsd">
        <bean id="demo" class="com.test.Demo"></bean>
        <bean id="myadvice" class="com.advice.MyAdvice"></bean>
        <aop:config>
        	<aop:aspect ref="myadvice">
        		<aop:pointcut expression="execution(* com.test.Demo.demo1(String,int)) and args(name,age)" id="mypoint"/>
         		<aop:before method="mybefore" pointcut-ref="mypoint" arg-names="name,age"/>
        		<aop:after method="myafter" pointcut-ref="mypoint"/>
        		<aop:after-returning method="myaftering" pointcut-ref="mypoint"/> 
        		<aop:after-throwing method="mythrow" pointcut-ref="mypoint"/>
        		<aop:around method="myarround" pointcut-ref="mypoint"/> 
        	</aop:aspect>
        </aop:config>
    </beans>
    ```

    ```java
    public class MyAdvice {
    	public void mybefore(String name , int age){
    		System.out.println("前置"+name+":"+age);
    	}
    	
    	public void myafter(){
    		System.out.println("myafter-后置");
    	}
    	public void myaftering(){
    		System.out.println("myaftering-后置");
    	}
    	public void mythrow(){
    		System.out.println("异常通知");
    	}
    	
    	public Object myarround(ProceedingJoinPoint p) throws Throwable{
    		System.out.println("执行环绕");
    		System.out.println("环绕--前置");
    		Object result = p.proceed();
    		System.out.println("环绕后置");
    		return result;
    	}
    }
    ```

#### 使用注解(基于 Aspect)

* spring不会自动去寻找注解，必须告诉spring哪些包下的类可能有注解

  * 引入 xmlns:context 命名空间
  * 扫描多个包使用，分隔

  ```xml
  <context:component-scan base-package="com.advice,com.test"></context:component-scan>
  ```

##### @Component

* 相当于 bean 标签
* 如果没有参数，把类名首字母变小写
* @Component("自定义名称")

#### 动态代理

* 特点：字节码随用随创建，随用随加载

* 作用：不修改源码的基础上对方法增强

* 基于接口的动态代理

  * 涉及的类：Proxy
  * 提供者：JDK官方

  * 如何创建代理对象
    * 使用Proxy类中的newProxyInstance方法
  * 创建代理对象的要求
    * 被代理类最少实现一个接口，如果没有则不能使用
  * newProxyInstance方法的参数
    * Classloader：类加载器
      * 它是用于加载代理对象字节码的。和被代理对象使用相同的类加载器。（固定写法）
    * Class[]：字节码数组
      * 它是用于让代理对象和被代理对象有相同的方法。（固定写法）
    * InvocationHandler：用于提供增强的代码
      * 它是让我们写如何代理，我们一般都是写一个该接口的实现类，通常情况下都是匿名内部类，但不是必须的。
      * 此接口的实现类都是谁用谁写。

  ```java
  		final Producer producer = new Producer();
  		IProducer proxyProducer = (IProducer) 					Proxy.newProxyInstance(producer.getClass().getClassLoader(),
  				producer.getClass().getInterfaces(),
  				new InvocationHandler() {
  					/**
  					 * 作用：执行被代理对象的任何接口方法都会经过该方法
  					 * @param proxy		代理对象的引用
  					 * @param method	当前执行的方法
  					 * @param args		当前执行方法所需的参数
  					 * @return			和被代理对象方法有相同的返回值
  					 * @throws Throwable
  					 */
  					public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
  						//提供增强的代码
  						Object returnValue = null;
  
  						//1.获取方法执行的参数
  						Float money = (Float)args[0];
  						//2.判断当前方法是不是销售
  						if("saleProduct".equals(method.getName())){
  							returnValue = method.invoke(producer,money*0.8f);
  						}
  						return returnValue;
  					}
  				});
  
  		proxyProducer.saleProduct(10000f);
  ```

  

* 基于子类的动态代理

  * 设计的类：Enhancer
  * 提供者：第三方cglib库
  * 如果创建代理对象
    * 使用Enhancer类中的create方法
  * 创建代理对象的要求
    * 被代理类不能是最终类
  * create方法的参数
    * Class：字节码
      * 它是用于被指定代理的字节码
    * Callback：用于提供增强的代码
      * 一般写的是该接口的子接口实现类：MethodInterceptor	

  ```java
  		final Producer producer = new Producer();
  
  		Producer cglibProducer = (Producer) Enhancer.create(producer.getClass(),
  				new MethodInterceptor() {
  					/**
  					 * 执行被代理对象的任何方法都会经过该方法
  					 * @param proxy			代理对象的引用
  					 * @param method		当前执行的方法
  					 * @param args			当前执行方法所需的参数
  					 * @param methodProxy	当前执行方法的代理对象
  					 * @return				和被代理对象方法有相同的返回值
  					 * @throws Throwable
  					 */
  					public Object intercept(Object proxy, Method method, Object[] args, MethodProxy methodProxy) throws Throwable {
  						//提供增强的代码
  						Object returnValue = null;
  
  						//1.获取方法执行的参数
  						Float money = (Float)args[0];
  						//2.判断当前方法是不是销售
  						if("saleProduct".equals(method.getName())){
  							returnValue = method.invoke(producer,money*0.8f);
  						}
  						return returnValue;
  					}
  				});
  		producer.saleProduct(10000);
  ```

  

