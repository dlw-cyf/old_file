#### 1. 尝试java配置

java配置主要靠java类和一些注解来达到和xml配置一样的效果，比较常用的注解有：

* @Configuration：声明一个类作为配置类，代替xml文件
* @Bean：声明在方法上，将方法的返回值加入Bean容器，代替<bean>标签
* @Value：属性注入
* @PropertySource：指定外部属性文件

##### 1.1 springboot注解配置DataSource

* 在application.properties文件写入属性

  * springboot启动默认读取这个文件

  ```properties
  jdbc.driverClassName=com.mysql.jdbc.Driver
  jdbc.url=jdbc:mysql://127.0.0.1:3306/ssm
  jdbc.username=root
  jdbc.password=123456
  ```

* 配置全局属性类

  * @ConfigurationProperties(prefix = "jdbc")：指定这是一个properties属性类
    * prefix：指定属性的前缀
  * 定义需要注入的属性
    * 属性名和配置文件的名字相同
    * 通过get/set方法进行注入

  ```java
  @ConfigurationProperties(prefix = "jdbc")
  public class JdbcProperties {
  
  	private String driverClassName;
  	private String url;
  	private String username;
  	private String password;
  
  	public String getDriverClassName() {
  		return driverClassName;
  	}
  
  	public void setDriverClassName(String driverClassName) {
  		this.driverClassName = driverClassName;
  	}
  
  	public String getUrl() {
  		return url;
  	}
  
  	public void setUrl(String url) {
  		this.url = url;
  	}
  
  	public String getUsername() {
  		return username;
  	}
  
  	public void setUsername(String username) {
  		this.username = username;
  	}
  
  	public String getPassword() {
  		return password;
  	}
  
  	public void setPassword(String password) {
  		this.password = password;
  	}
  }
  ```

* 定义配置类

  * @Configuration：声明这是一个配置类
  * @EnableConfigurationProperties({JdbcProperties.class})：指定需要读取的属性类
    * value为数组类型，可以读取多个属性类

  ```java
  @Configuration //声明配置类
  @EnableConfigurationProperties({JdbcProperties.class})
  public class JdbcConfiguration {
  
  	@Autowired
  	private JdbcProperties jdbcProperties;
      
      使用构造方法进行注入
      public JdbcConfiguration(JdbcProperties jdbcProperties){
  		this.jdbcProperties = jdbcProperties;
  	}
  
      /**
      *使用形参注入
      */
  	@Bean  //把方法的返回值注入到spring容器
  	public DataSource dataSource(JdbcProperties jdbcProperties){
  		DruidDataSource dataSource = new DruidDataSource();
  		dataSource.setDriverClassName(jdbcProperties.getDriverClassName());
  		dataSource.setUrl(jdbcProperties.getUrl());
  		dataSource.setUsername(jdbcProperties.getUsername());
  		dataSource.setPassword(jdbcProperties.getPassword());
  		return dataSource;
  	}
  
  }
  ```

##### 1.2. 最优雅的配置

* 在方法上使用@ConfigurationProperties(prefix = "jdbc")
* dataSource的属性会进行自动注入
  * 使用dataSource的get/set方法

```java
@Configuration //声明配置类
public class JdbcConfiguration {

	@Bean  //把方法的返回值注入到spring容器
	@ConfigurationProperties(prefix = "jdbc")
	public DataSource dataSource(){
		DruidDataSource dataSource = new DruidDataSource();
		return dataSource;
	}

}
```



### 2.SpringBoot热部署

#### 2.1 添加pom依赖

```xml
<dependency>
	<groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-devtools</artifactId>
</dependency>
```

#### 2.2 注意：idea热部署失败原因

* 出现这种情况，并不是热部署问题，其根本原始是因为IDEA默认情况下不会自动编译，需要对IDEA进行自动设置