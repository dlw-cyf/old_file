##### 介绍

* 由apache推出对 开元免费日志处理的类库

##### 为什么需要日志

* 在项目中编写System.out.println();输出到控制台，当项目发布到tomact后，没有控制台(在命令行界面能看见)，不容易观察一些输出结果

* log4j作用：不仅能把内容输出到控制台，还能吧内容输出到文件中，便于观察结果

##### 使用步骤

1. 导入log4j-xxx.jar

2. 在src下新建log4j.properties（路径和名称都不允许改变）

3. 使用log4j输出日志

   ```java
       Logger logger = Logger.getLogger(Test.class);
   
       logger.debug("这是一个调试信息");
       logger.info("普通信息");
   ```

##### log4j的输出级别

* fatal（致命错误）
* error（错误）
* warn（警告）
* info（普通信息）
* debug（调试信息）

##### log4j   命名级别

* 包级别

```properties
log4j.logger.包名=DEBUG
```

* 类级别

```properties
log4j.logger.包名.类名=DEBUG
```

* 方法级别

```properties
log4j.logger.包名.类名.方法名=DEBUG
```

##### log4j.properties文件内容

```properties
log4j.rootCategory=INFO,CONSOLE

log4j.appender.CONSOLE=org.apache.log4j.ConsoleAppender
log4j.appender.CONSOLE.layout=org.apache.log4j.PatternLayout
log4j.appender.CONSOLE.layout.ConversionPattern=%C %d{YYYY-MM--dd HH:mm:ss} %m %n

log4j.appender.LOGFILE=org.apache.log4j.FileAppender
log4j.appender.LOGFILE.File=axis.log
log4j.appender.LOGFILE.Append=true
log4j.appender.LOGFILE.layout=org.apache.log4j.PatternLayout
log4j.appender.LOGFILE.layout.ConversionPattern=%C %m %n
```

