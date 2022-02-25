##### environments标签

* 配置数据库的连接

```xml
	<!-- default引用environment的id，当前所使用的环境 -->
	<environments default="default">
		<environment id="default">
			<!-- 使用原生JDBC事务 -->
			<transactionManager type="JDBC"></transactionManager>
			<!-- 数据库连接池 -->
			<dataSource type="POOLED">
				<property name="driver" value="com.mysql.jdbc.Driver"/>
				<property name="url" value="jdbc:mysql://127.0.0.1:3306/ssm"/>
				<property name="username" value="root"/>
				<property name="password" value="123456"/>
			</dataSource>
		</environment>
	</environments>
```

##### mappers标签

* 添加配置文件的映射，加载mapper.xml配置文件

```xml
	<!-- 加载配置文件 -->
	<mappers>
		<mapper resource="com/dlw/mapper/FlowerMapper.xml"/>
	</mappers>
```

##### settings标签

* 控制mybatis全局开关

* 在mybatis.xml中开启log4j

  * 必须保证有log4j.jar
  * 在src下有log4j.properties文件

  ```xml
      <settings>
          <!-- 开启mybatis的log4j支持 -->
          <setting name="logImpl" value="LOG4J"/>
      </settings>
  ```

##### typeAliases标签

* typeAlias

  * 给类起别名

    ```xml
    <typeAliases>
        <typeAlias type="com.dlw.pojo.Flower" alias="flower"/>
    </typeAliases>
    ```

* package

  * 为指定的包下所有的类别名
    * 别名==类名小写

  ```xml
  <typeAliases>
      <package name="com.dlw.pojo"/>
  </typeAliases>
  ```

* 在Mapper.xml中使用别名引用类

